package com.example.wms.business.analysis.service.impl;

import com.example.wms.business.analysis.dto.req.ReplenishmentReq;
import com.example.wms.business.analysis.dto.rsp.ReplenishmentRsp;
import com.example.wms.business.analysis.mapper.ReplenishmentMapper;
import com.example.wms.business.analysis.service.ReplenishmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplenishmentServiceImpl implements ReplenishmentService {

    private final ReplenishmentMapper replenishmentMapper;

    /** 缺货阈值：支撑天数 < 6 */
    private static final BigDecimal SHORTAGE_DAYS = new BigDecimal("6");
    /** 紧张阈值：支撑天数 < 15 */
    private static final BigDecimal TENSE_DAYS = new BigDecimal("15");

    @Override
    public List<ReplenishmentRsp> listReplenishment(ReplenishmentReq req) {
        if (req.getHistoryDays() == null || req.getHistoryDays() <= 0) {
            req.setHistoryDays(30);
        }
        if (req.getForecastDays() == null || req.getForecastDays() <= 0) {
            req.setForecastDays(15);
        }
        if (req.getDimension() == null || req.getDimension().isBlank()) {
            req.setDimension("sku");
        }

        boolean bySku = "sku".equalsIgnoreCase(req.getDimension());
        List<ReplenishmentRsp> list = bySku
                ? replenishmentMapper.selectBySku(req)
                : replenishmentMapper.selectByInnerCode(req);

        for (ReplenishmentRsp row : list) {
            compute(row, req.getForecastDays(), req.getHistoryDays());
        }

        // 默认排序：支撑天数升序（最紧急在前），建议采购量降序次之
        list.sort((a, b) -> {
            BigDecimal daysA = a.getSupportDays() == null ? new BigDecimal("9999") : a.getSupportDays();
            BigDecimal daysB = b.getSupportDays() == null ? new BigDecimal("9999") : b.getSupportDays();
            int cmp = daysA.compareTo(daysB);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(b.getSuggestQty(), a.getSuggestQty());
        });

        return list;
    }

    private void compute(ReplenishmentRsp row, int forecastDays, int historyDays) {
        int historyQty = row.getHistoryQty() == null ? 0 : row.getHistoryQty();
        int availableQty = row.getAvailableQty() == null ? 0 : row.getAvailableQty();

        // 日均出库量（保留4位小数）
        BigDecimal dailyAvg = historyDays > 0 && historyQty > 0
                ? new BigDecimal(historyQty).divide(new BigDecimal(historyDays), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        row.setDailyAvgQty(dailyAvg);

        // 预测需求量（向上取整）
        int forecastQty = dailyAvg.compareTo(BigDecimal.ZERO) > 0
                ? dailyAvg.multiply(new BigDecimal(forecastDays)).setScale(0, RoundingMode.CEILING).intValue()
                : 0;
        row.setForecastQty(forecastQty);

        // 建议采购量
        int suggestQty = Math.max(0, forecastQty - availableQty);
        row.setSuggestQty(suggestQty);

        // 库存支撑天数
        if (dailyAvg.compareTo(BigDecimal.ZERO) <= 0) {
            row.setSupportDays(null);
            row.setStatus(1); // 充足（无出库记录）
        } else {
            BigDecimal supportDays = new BigDecimal(availableQty)
                    .divide(dailyAvg, 2, RoundingMode.HALF_UP);
            row.setSupportDays(supportDays);
            if (availableQty == 0 || supportDays.compareTo(SHORTAGE_DAYS) < 0) {
                row.setStatus(3);
            } else if (supportDays.compareTo(TENSE_DAYS) < 0) {
                row.setStatus(2);
            } else {
                row.setStatus(1);
            }
        }

        // 建议采购金额
        BigDecimal defaultCost = row.getDefaultCost() == null ? BigDecimal.ZERO : row.getDefaultCost();
        row.setSuggestAmount(defaultCost.multiply(new BigDecimal(suggestQty)).setScale(2, RoundingMode.HALF_UP));
    }
}
