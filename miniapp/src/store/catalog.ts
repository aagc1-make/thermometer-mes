import { create } from 'zustand';
import type { Product } from '@/types';
import { PRODUCTS } from '@/data/products';
import { loadCatalog } from '@/services/api';

interface CatalogState {
  products: Product[];
  /** 是否已成功从后端加载 */
  loaded: boolean;
  loading: boolean;
  load: () => Promise<void>;
}

/**
 * 产品目录：与 MES 主数据一致（成品 1 / 半成品 2 / 物料 14）
 */
export const useCatalogStore = create<CatalogState>()((set, get) => ({
  products: PRODUCTS,
  loaded: false,
  loading: false,

  load: async () => {
    if (get().loading || get().loaded) {
      return;
    }
    set({ loading: true });
    try {
      const products = await loadCatalog();
      set({ products, loading: false, loaded: true });
      console.info('[Catalog] 产品目录已加载', products.length, '款');
    } catch (error) {
      console.error('[Catalog] 产品目录加载失败', error);
      set({ loading: false });
    }
  }
}));
