/*
 * Copyright 2017 NAVER Corp.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.web.service.stat;

import com.navercorp.pinpoint.common.server.bo.stat.join.JoinDataSourceListBo.DataSourceKey;
import com.navercorp.pinpoint.common.service.ServiceTypeRegistryService;
import com.navercorp.pinpoint.web.dao.ApplicationDataSourceDao;
import com.navercorp.pinpoint.web.util.TimeWindow;
import com.navercorp.pinpoint.web.vo.stat.AggreJoinDataSourceBo;
import com.navercorp.pinpoint.web.vo.stat.AggreJoinDataSourceListBo;
import com.navercorp.pinpoint.web.vo.stat.chart.ApplicationDataSourceChartGroup;
import com.navercorp.pinpoint.web.vo.stat.chart.ApplicationStatChartGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author minwoo.jung
 */
@Service
public class ApplicationDataSourceService {

    @Autowired
    private ApplicationDataSourceDao applicationDataSourceDao;

    @Autowired
    private ServiceTypeRegistryService serviceTypeRegistryService;

    private static final AggreJoinDataSourceBoComparator comparator = new AggreJoinDataSourceBoComparator();

    public List<ApplicationStatChartGroup> selectApplicationChart(String applicationId, TimeWindow timeWindow) {
        if (applicationId == null) {
            throw new NullPointerException("applicationId must not be null");
        }
        if (timeWindow == null) {
            throw new NullPointerException("timeWindow must not be null");
        }

        List<ApplicationStatChartGroup> result = new ArrayList<ApplicationStatChartGroup>();
        List<AggreJoinDataSourceListBo> aggreJoinDataSourceListBoList = this.applicationDataSourceDao.getApplicationStatList(applicationId, timeWindow);

        if (aggreJoinDataSourceListBoList.size() == 0) {
            result.add(new ApplicationDataSourceChartGroup(timeWindow, "", "", Collections.EMPTY_LIST));
            return result;
        }

        Map<DataSourceKey, List<AggreJoinDataSourceBo>> aggreJoinDataSourceBoMap = classifyByDataSourceUrl(aggreJoinDataSourceListBoList);

        for (Map.Entry<DataSourceKey, List<AggreJoinDataSourceBo>> entry: aggreJoinDataSourceBoMap.entrySet()) {
            DataSourceKey dataSourceKey = entry.getKey();
            String serviceTypeName = serviceTypeRegistryService.findServiceType((short) dataSourceKey.getServiceTypeCode()).getName();
            result.add(new ApplicationDataSourceChartGroup(timeWindow, dataSourceKey.getUrl(), serviceTypeName, entry.getValue()));
        }

        return result;
    }


    protected Map<DataSourceKey, List<AggreJoinDataSourceBo>> classifyByDataSourceUrl(List<AggreJoinDataSourceListBo> aggreJoinDataSourceListBoList) {

        Map<DataSourceKey, List<AggreJoinDataSourceBo>> aggreJoinDataSourceBoMap = new HashMap<>();

        for (AggreJoinDataSourceListBo aggreJoinDataSourceListBo : aggreJoinDataSourceListBoList) {
            for (AggreJoinDataSourceBo aggreJoinDataSourceBo : aggreJoinDataSourceListBo.getAggreJoinDataSourceBoList()) {
                DataSourceKey dataSourceKey = new DataSourceKey(aggreJoinDataSourceBo.getUrl(), aggreJoinDataSourceBo.getServiceTypeCode());
                List<AggreJoinDataSourceBo> aggreJoinDataSourceBoList = aggreJoinDataSourceBoMap.get(dataSourceKey);

                if (aggreJoinDataSourceBoList == null) {
                    aggreJoinDataSourceBoList = new ArrayList<AggreJoinDataSourceBo>();
                    aggreJoinDataSourceBoMap.put(dataSourceKey, aggreJoinDataSourceBoList);
                }

                aggreJoinDataSourceBoList.add(aggreJoinDataSourceBo);
            }
        }

        for(List<AggreJoinDataSourceBo> aggreJoinDataSourceBoList : aggreJoinDataSourceBoMap.values()) {
            Collections.sort(aggreJoinDataSourceBoList, comparator);
        }

        return aggreJoinDataSourceBoMap;
    }

    private static class AggreJoinDataSourceBoComparator implements Comparator<AggreJoinDataSourceBo> {
        @Override
        public int compare(AggreJoinDataSourceBo bo1, AggreJoinDataSourceBo bo2) {
            return bo1.getTimestamp() < bo2.getTimestamp() ? -1 : 1;
        }
    }
}
