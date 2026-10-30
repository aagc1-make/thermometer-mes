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
 * 产品目录：初始使用本地演示数据，进入页面后异步拉取后端真实目录
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
