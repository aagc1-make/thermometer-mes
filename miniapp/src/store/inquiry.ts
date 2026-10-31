import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { StateStorage } from 'zustand/middleware';
import Taro from '@tarojs/taro';
import dayjs from 'dayjs';
import type { InquiryItem, InquiryMeta, InquiryRecord, Product } from '@/types';
import { DEFAULT_INQUIRY_META } from '@/data/customer';
import { CUSTOMER } from '@/data/customer';
import { cancelInquiryRemote, submitInquiryRemote } from '@/services/api';

/** Taro 存储适配器，用于询价单本地持久化 */
const taroStorage: StateStorage = {
  getItem: (name: string) => {
    const value = Taro.getStorageSync(name);
    return value || null;
  },
  setItem: (name: string, value: string) => {
    Taro.setStorageSync(name, value);
  },
  removeItem: (name: string) => {
    Taro.removeStorageSync(name);
  }
};

function toInquiryItem(product: Product, qty: number): InquiryItem {
  return {
    productId: product.id,
    name: product.name,
    code: product.code,
    categoryName: product.categoryName,
    art: product.art,
    qty,
    moq: product.moq,
    unit: product.unit
  };
}

/** 待提交草稿：默认不含演示商品，由用户自行加购 */
const INITIAL_DRAFT: InquiryItem[] = [];

/** 已提交（待报价）历史记录，实际数据从后端加载 */
const INITIAL_SUBMITTED: InquiryRecord[] = [];

/** 已报价历史记录，与后端询价单保持同步 */
const INITIAL_QUOTED: InquiryRecord[] = [
  {
    id: 'XJ20261019-001',
    status: 'quoted',
    items: [
      {
        productId: '9218',
        name: '基础款红外测温枪',
        code: 'IRG-FG-001',
        categoryName: '成品',
        art: 'thermometer',
        qty: 100,
        moq: 50,
        unit: '件'
      },
      {
        productId: '9223',
        name: '测温显示电控预装件',
        code: 'IRG-SA-CTRL-001',
        categoryName: '成品',
        art: 'pcb',
        qty: 200,
        moq: 100,
        unit: '件'
      }
    ],
    itemCount: 2,
    totalQty: 300,
    submitTime: '2026-10-19 09:30',
    expectedDate: '2026-11-15',
    address: DEFAULT_INQUIRY_META.address,
    invoice: DEFAULT_INQUIRY_META.invoice,
    remark: '报价含税含运费，有效期 7 天',
    quoteAmount: 74000
  }
];

interface InquiryState {
  draftId: string;
  items: InquiryItem[];
  meta: InquiryMeta;
  submitted: InquiryRecord[];
  quoted: InquiryRecord[];
  /** 已在小程序侧撤销的询价单号（服务端同步置为 CANCELED） */
  cancelledIds: string[];
  addItem: (product: Product, qty?: number) => void;
  removeItem: (productId: string) => void;
  setQty: (productId: string, qty: number) => void;
  clearDraft: () => void;
  updateMeta: (meta: Partial<InquiryMeta>) => void;
  cancelRecord: (id: string) => Promise<void>;
  submit: () => Promise<InquiryRecord | null>;
}

function nextDraftId(seq: number): string {
  return `XJ${dayjs().format('YYYYMMDD')}-${String(seq).padStart(3, '0')}`;
}

export const useInquiryStore = create<InquiryState>()(
  persist(
    (set, get) => ({
      draftId: 'XJ20261024-001',
      items: INITIAL_DRAFT,
      meta: DEFAULT_INQUIRY_META,
      submitted: INITIAL_SUBMITTED,
      quoted: INITIAL_QUOTED,
      cancelledIds: [],

      addItem: (product, qty) => {
        const list = get().items;
        const addQty = qty ?? product.moq;
        const exist = list.find((item) => item.productId === product.id);
        const nextItems = exist
          ? list.map((item) =>
              item.productId === product.id ? { ...item, qty: item.qty + addQty } : item
            )
          : [...list, toInquiryItem(product, addQty)];
        set({ items: nextItems });
        console.info('[Inquiry] 加入询价单', product.code, addQty, '当前品项数', nextItems.length);
      },

      removeItem: (productId) => {
        const nextItems = get().items.filter((item) => item.productId !== productId);
        set({ items: nextItems });
        console.info('[Inquiry] 移除商品', productId, '剩余品项数', nextItems.length);
      },

      setQty: (productId, qty) => {
        const nextItems = get().items.map((item) =>
          item.productId === productId ? { ...item, qty: Math.max(1, qty) } : item
        );
        set({ items: nextItems });
      },

      clearDraft: () => {
        set({ items: [] });
        console.info('[Inquiry] 清空询价单');
      },

      updateMeta: (meta) => {
        set({ meta: { ...get().meta, ...meta } });
      },

      cancelRecord: async (id) => {
        try {
          // 服务端把状态置为 CANCELED（记录保留，可追溯）
          await cancelInquiryRemote(id);
        } catch (error) {
          console.info('[Inquiry] 服务端撤销失败，仅本地标记', error);
        }
        const next = Array.from(new Set([...get().cancelledIds, id]));
        set({ cancelledIds: next });
        console.info('[Inquiry] 撤销询价单', id);
      },

      submit: async () => {
        const { items, meta, submitted, draftId } = get();
        if (items.length === 0) {
          return null;
        }
        const record: InquiryRecord = {
          id: draftId,
          status: 'submitted',
          items: items.map((item) => ({ ...item })),
          itemCount: items.length,
          totalQty: items.reduce((sum, item) => sum + item.qty, 0),
          submitTime: dayjs().format('YYYY-MM-DD HH:mm'),
          expectedDate: meta.expectedDate,
          address: meta.address,
          invoice: meta.invoice,
          remark: meta.remark
        };

        // 优先写入后端，失败时保留本地提交（不影响小程序继续使用）
        try {
          const result = await submitInquiryRemote({
            clientName: CUSTOMER.company,
            buyer: CUSTOMER.buyer,
            expectedDate: meta.expectedDate,
            deliveryAddress: meta.address,
            invoiceType: meta.invoice,
            remark: meta.remark,
            lines: items.map((item) => {
              const itemId = Number(item.productId);
              return {
                itemId: Number.isFinite(itemId) ? itemId : undefined,
                itemCode: item.code,
                itemName: item.name,
                qty: item.qty,
                unitName: item.unit,
                moq: item.moq
              };
            })
          });
          if (result?.inquiryNo) {
            record.id = result.inquiryNo;
            console.info('[Inquiry] 已写入后端', result.inquiryNo, result.inquiryId);
          }
        } catch (error) {
          console.info('[Inquiry] 后端询价接口不可用，已保存到本地', error);
        }

        const nextSubmitted = [record, ...submitted];
        set({
          items: [],
          submitted: nextSubmitted,
          draftId: nextDraftId(nextSubmitted.length + 1)
        });
        console.info('[Inquiry] 询价单提交完成', record.id, record.totalQty);
        return record;
      }
    }),
    {
      name: 'ktg-inquiry-store',
      storage: createJSONStorage(() => taroStorage),
      partialize: (state) => ({
        draftId: state.draftId,
        items: state.items,
        meta: state.meta,
        submitted: state.submitted,
        quoted: state.quoted,
        cancelledIds: state.cancelledIds
      })
    }
  )
);
