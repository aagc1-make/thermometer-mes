import React, { useEffect, useState } from 'react';
import { Input, Text, View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import {
  createAddress,
  fetchAddresses,
  makeDefaultAddress,
  removeAddress,
  updateAddress
} from '@/services/api';
import { useInquiryStore } from '@/store/inquiry';
import type { Address, AddressPayload } from '@/types';
import styles from './index.module.scss';

interface FormState {
  contactName: string;
  contactTel: string;
  region: string;
  detail: string;
  tag: string;
  isDefault: boolean;
}

const EMPTY_FORM: FormState = {
  contactName: '',
  contactTel: '',
  region: '',
  detail: '',
  tag: '',
  isDefault: false
};

const AddressPage: React.FC = () => {
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [loading, setLoading] = useState(true);
  const [formVisible, setFormVisible] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);

  const load = async () => {
    setLoading(true);
    const list = await fetchAddresses();
    setAddresses(list);
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  const patch = (key: keyof FormState, value: string | boolean) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const openCreate = () => {
    setEditingId(null);
    setForm({ ...EMPTY_FORM, isDefault: addresses.length === 0 });
    setFormVisible(true);
  };

  const openEdit = (item: Address) => {
    setEditingId(item.id);
    setForm({
      contactName: item.contactName,
      contactTel: item.contactTel,
      region: item.region,
      detail: item.detail,
      tag: item.tag || '',
      isDefault: item.isDefault
    });
    setFormVisible(true);
  };

  /** 后端不可用时的本地变更 */
  const applyLocal = (payload: AddressPayload, id?: number) => {
    setAddresses((prev) => {
      const cleared = payload.isDefault
        ? prev.map((item) => ({ ...item, isDefault: false }))
        : prev;
      if (id) {
        return cleared.map((item) =>
          item.id === id ? { ...item, ...payload, id, isDefault: payload.isDefault || item.isDefault } : item
        );
      }
      const nextId = Date.now();
      return [
        ...cleared,
        {
          id: nextId,
          contactName: payload.contactName,
          contactTel: payload.contactTel,
          region: payload.region,
          detail: payload.detail,
          tag: payload.tag,
          isDefault: payload.isDefault || prev.length === 0
        }
      ];
    });
  };

  const handleSave = async () => {
    if (!form.contactName || !form.contactTel || !form.region || !form.detail) {
      Taro.showToast({ title: '请完整填写收货信息', icon: 'none' });
      return;
    }
    const payload: AddressPayload = {
      contactName: form.contactName.trim(),
      contactTel: form.contactTel.trim(),
      region: form.region.trim(),
      detail: form.detail.trim(),
      tag: form.tag.trim() || undefined,
      isDefault: form.isDefault
    };
    try {
      if (editingId) {
        await updateAddress(editingId, payload);
      } else {
        await createAddress(payload);
      }
      Taro.showToast({ title: '地址已保存', icon: 'success' });
      setFormVisible(false);
      await load();
    } catch (error) {
      console.info('[Address] 后端不可用，保存到本地', error);
      applyLocal(payload, editingId || undefined);
      setFormVisible(false);
      Taro.showToast({ title: '已保存到本地', icon: 'none' });
    }
  };

  const handleDefault = async (item: Address) => {
    if (item.isDefault) {
      return;
    }
    try {
      await makeDefaultAddress(item.id);
      setAddresses((prev) => prev.map((row) => ({ ...row, isDefault: row.id === item.id })));
      Taro.showToast({ title: '已设为默认地址', icon: 'success' });
    } catch (error) {
      console.info('[Address] 设为默认失败，本地生效', error);
      setAddresses((prev) => prev.map((row) => ({ ...row, isDefault: row.id === item.id })));
      Taro.showToast({ title: '已在本地设为默认', icon: 'none' });
    }
  };

  const handleDelete = async (item: Address) => {
    const result = await Taro.showModal({
      title: '删除地址',
      content: `确认删除「${item.contactName} · ${item.region} ${item.detail}」吗？`,
      confirmText: '删除',
      confirmColor: '#f04438'
    });
    if (!result.confirm) {
      return;
    }
    try {
      await removeAddress(item.id);
    } catch (error) {
      console.info('[Address] 删除接口不可用，本地删除', error);
    }
    setAddresses((prev) => {
      const rest = prev.filter((row) => row.id !== item.id);
      if (item.isDefault && rest.length > 0) {
        rest[0].isDefault = true;
      }
      return rest;
    });
    Taro.showToast({ title: '已删除', icon: 'none' });
  };

  const useAddress = (item: Address) => {
    useInquiryStore.getState().updateMeta({ address: `${item.region} ${item.detail}` });
    Taro.showToast({ title: '已用于本次询价', icon: 'success' });
    setTimeout(() => {
      Taro.navigateBack().catch(() => undefined);
    }, 500);
  };

  return (
    <View className={styles.page}>
      <PageHeader title="收货地址" showBack rightText="新增" rightIcon="plus" onRightClick={openCreate} />

      {formVisible && (
        <View className={styles.formCard}>
          <View className={styles.formHead}>
            <Text className={styles.formTitle}>{editingId ? '编辑收货地址' : '新增收货地址'}</Text>
            <View className={styles.close} onClick={() => setFormVisible(false)}>
              <Icon name="close" size={24} color="#8a99b3" />
            </View>
          </View>
          <View className={styles.field}>
            <Text className={styles.fieldLabel}>收货人</Text>
            <Input
              className={styles.input}
              value={form.contactName}
              placeholder="请输入收货人姓名"
              onInput={(event) => patch('contactName', event.detail.value)}
            />
          </View>
          <View className={styles.field}>
            <Text className={styles.fieldLabel}>联系电话</Text>
            <Input
              className={styles.input}
              value={form.contactTel}
              placeholder="请输入联系电话"
              onInput={(event) => patch('contactTel', event.detail.value)}
            />
          </View>
          <View className={styles.field}>
            <Text className={styles.fieldLabel}>所在地区</Text>
            <Input
              className={styles.input}
              value={form.region}
              placeholder="如：江苏省苏州市"
              onInput={(event) => patch('region', event.detail.value)}
            />
          </View>
          <View className={styles.field}>
            <Text className={styles.fieldLabel}>详细地址</Text>
            <Input
              className={styles.input}
              value={form.detail}
              placeholder="街道、门牌号、楼层"
              onInput={(event) => patch('detail', event.detail.value)}
            />
          </View>
          <View className={styles.field}>
            <Text className={styles.fieldLabel}>标签</Text>
            <Input
              className={styles.input}
              value={form.tag}
              placeholder="如：收货点 / 备用"
              onInput={(event) => patch('tag', event.detail.value)}
            />
          </View>
          <View className={styles.switchRow} onClick={() => patch('isDefault', !form.isDefault)}>
            <Text className={styles.switchLabel}>设为默认收货地址</Text>
            <View className={classnames(styles.switch, form.isDefault && styles.switchOn)}>
              <View className={classnames(styles.switchDot, form.isDefault && styles.switchDotOn)} />
            </View>
          </View>
          <View className={styles.saveBtn} onClick={handleSave}>
            <Icon name="check" size={24} color="#ffffff" className={styles.btnIcon} />
            <Text className={styles.saveText}>保存地址</Text>
          </View>
        </View>
      )}

      {loading && (
        <View className={styles.empty}>
          <Text className={styles.emptyText}>加载中...</Text>
        </View>
      )}

      {!loading &&
        addresses.map((item) => (
          <View key={item.id} className={styles.card}>
            <View className={styles.cardHead}>
              <View className={styles.headLeft}>
                <View className={styles.pinWrap}>
                  <Icon name="pin" size={30} color="#2b6beb" />
                </View>
                <View className={styles.nameWrap}>
                  <View className={styles.nameRow}>
                    <Text className={styles.name}>{item.contactName}</Text>
                    {item.isDefault && <Text className={styles.defaultTag}>默认</Text>}
                    {item.tag && item.tag !== '默认' && <Text className={styles.tag}>{item.tag}</Text>}
                  </View>
                  <Text className={styles.tel}>{item.contactTel}</Text>
                </View>
              </View>
            </View>
            <Text className={styles.address}>
              {item.region} {item.detail}
            </Text>
            <View className={styles.divider} />
            <View className={styles.actions}>
              {!item.isDefault && (
                <View className={styles.action} onClick={() => handleDefault(item)}>
                  <Icon name="check" size={20} color="#5a6b87" className={styles.actionIcon} />
                  <Text className={styles.actionText}>设为默认</Text>
                </View>
              )}
              <View className={styles.action} onClick={() => openEdit(item)}>
                <Icon name="edit" size={20} color="#5a6b87" className={styles.actionIcon} />
                <Text className={styles.actionText}>编辑</Text>
              </View>
              <View className={classnames(styles.action, styles.actionDanger)} onClick={() => handleDelete(item)}>
                <Icon name="trash" size={20} color="#f04438" className={styles.actionIcon} />
                <Text className={styles.actionTextDanger}>删除</Text>
              </View>
              <View className={styles.useBtn} onClick={() => useAddress(item)}>
                <Icon name="pin" size={22} color="#2b6beb" className={styles.actionIcon} />
                <Text className={styles.useText}>使用该地址</Text>
              </View>
            </View>
          </View>
        ))}

      {!loading && addresses.length === 0 && (
        <View className={styles.emptyCard}>
          <Icon name="pin" size={56} color="#2b6beb" />
          <Text className={styles.emptyTitle}>还没有收货地址</Text>
          <Text className={styles.emptyDesc}>添加常用收货地址，询价与下单时可直接选择</Text>
          <View className={styles.emptyBtn} onClick={openCreate}>
            <Icon name="plus" size={24} color="#ffffff" className={styles.btnIcon} />
            <Text className={styles.emptyBtnText}>新增地址</Text>
          </View>
        </View>
      )}
    </View>
  );
};

export default AddressPage;
