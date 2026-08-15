package com.familykitchen.order.model.bo;

/**
 * 表示配送Snapshot领域计算过程中的业务数据。
 *
 * @param contactName 联系人名称
 * @param contactPhone 联系人联系电话
 * @param addressText 地址Text
 */
public record DeliverySnapshot(
    String contactName,
    String contactPhone,
    String addressText
) {
}
