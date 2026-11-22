package com.ktg.mes.wm.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ktg.mes.wm.domain.WmMaterialStock;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 工单备料检查与可供勾选的批次。数量始终按物料的基本单位返回。 */
public class WorkorderShortagePlan {
    public String workorderCode;
    public boolean closed;
    public String requestId;
    public List<Material> materials;
    public List<Document> documents;

    public static class Material {
        public Long itemId;
        public String itemCode;
        public String itemName;
        public String unit;
        public BigDecimal required;
        public BigDecimal issued;
        public BigDecimal onHand;
        public BigDecimal missing;
        public BigDecimal uncovered;
        public final List<Source> sources = new ArrayList<>();
    }

    public static class Source {
        public Long stockId;
        public String warehouse;
        public String location;
        public String area;
        public String batch;
        public BigDecimal available;
        public BigDecimal quantity;
        public String version;
        @JsonIgnore public WmMaterialStock stock;
    }

    public static class Document {
        public Long transferId;
        public String transferCode;
        public String status;
        @JsonIgnore public String operation;
        public boolean isActive() {
            return !("CANCELED".equals(status) || "FINISHED".equals(status));
        }
    }

    public static class Confirmation {
        public String requestId;
        public List<Selection> lines;
    }

    public static class Selection {
        public Long stockId;
        public String version;
    }
}
