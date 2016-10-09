package other.aakepi.bdfjfaackcpic.api.media;

import com.rkhd.platform.sdk.api.ApiSupport;
import com.rkhd.platform.sdk.http.Request;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import other.aakepi.bdfjabipeepce.util.DateUtil;
import other.aakepi.bdfjfaackcpic.api.QueryResult;
import other.aakepi.bdfjfaackcpic.config.SpotField;

import java.util.*;

/**
 * 销售合同排期查询
 * Created by yangyixin on 16/10/8.
 */
public class SaleContractSpotSearch extends BaseSpotSearch implements ApiSupport {

    //合同ID
    protected  String contractId;

    protected  JSONObject contract;
    protected  Date startDate;
    protected  Date endDate;
    /**
     * 初始化显示字段
     */
    protected void initSpotConfig() {
        //添加显示的字段：
        spotConfig.addField("name", 150, null);
        spotConfig.addField("form", 80, null);
        spotConfig.addField("entityType", 80, null);
        spotConfig.addField("opMode", 80, null);
        spotConfig.addField("saleStatus", 80, null);
        spotConfig.addField("address", 150, null);

        spotConfig.addFieldNotMedia("onlineTimeArange", "点位上线时间段", 100, null);
//        spotConfig.addFieldNotMedia("boughtType", "购买单位形式", 100, null);

        //在单价后显示排期点位信息
        spotConfig.setShowSpotFieldName("onlineTimeArange");
    }

    @Override
    public String execute(Request request, Long userId, Long tenantId) {

        initRequest(request);
        contractId= request.getParameter("id");
        if (StringUtils.isBlank(contractId)) contractId="-1";
        JSONObject contract = getContract(contractId);

        Map<String, Object> returnMap = new HashMap<String, Object>();

        returnMap.put("contract", contract);
        //合同不为空
        if (!contract.isEmpty() && !contract.containsKey("error_code")){
            initParam();

            startDate = DateUtil.getDate(contract.getString("startDate"));
            endDate = DateUtil.getDate(contract.getString("endDate"));

            JSONObject sheetObj = sheet(startDate, endDate, null);
            returnMap.put("sheet", sheetObj);
        }

        return JSONObject.fromObject(returnMap).toString();
    }

    /**
     * 查询合同
     * @return
     */
    protected JSONObject getContract( String contractId) {
        StringBuffer sql = new StringBuffer();
        RkhdHttpData rkhdHttpData = postRkhdHttpData("/data/v1/objects/contract/info");
        rkhdHttpData.putFormData("id", contractId);
        String result = apiRequest(request, rkhdHttpData);
        return  JSONObject.fromObject(result);
    }



    /**
     * 获得媒体和广告位的JSON数组
     *
     * @param first
     * @param size
     * @return
     */
    protected JSONArray getMediaSpotCellData( int first, int size, int sheetId) {
        JSONArray headData = new JSONArray();
        JSONArray records = getAllMedia(first, size);
        if (records == null) return headData;
        JSONArray spotPlanDateList ;
        for (int i = 0; i < records.size(); i++) {
            //从第四行开始
            int startRow = 4 + i;
            int dateColumns = 0;
            JSONObject record = records.getJSONObject(i);
            String spotId =record.getString("id");
            JSONObject col0 = getItemObject(sheetId, startRow, dateColumns++);
            col0.accumulate("json", "{id: \"" + spotId + "\"}");
            headData.add(col0);

            List<SpotField> spotFieldList = spotConfig.getSpotFielList();
            for (int j = 0; j < spotFieldList.size(); j++) {
                SpotField spotField = spotFieldList.get(j);
                String fieldName = spotField.getEn();
                Object value = record.get(fieldName);
                if (value==null || StringUtils.isBlank(value.toString())){
                    //不处理
                    dateColumns++;
                }  else{
                    if ("select".equals(spotField.getType()) || "entityType".equals(spotField.getType())) {
                        headData.add(getColItemSelect(sheetId, startRow, dateColumns++, record, fieldName));
                    } else if (fieldName.equals("name")){//媒体名称，增加ID
                        Object meidaId = record.get("meidaId");
                        headData.add(getColItemIdValue(sheetId, startRow, dateColumns++, meidaId, value));
                    } else {
                        headData.add(getColItemObject(sheetId, startRow, dateColumns++, value));
                    }
                }
                //显示点位
                boolean renderDateColumn = fieldName.equalsIgnoreCase(spotConfig.getShowSpotFieldName());
                if (renderDateColumn) {
                    spotPlanDateList = getSpotDate(spotId);
                    //------------------------------------------
                    if (startDate != null && endDate != null) {
                        Calendar startCal = Calendar.getInstance();
                        startCal.setTime(startDate);
                        Calendar endCal = Calendar.getInstance();
                        endCal.setTime(endDate);
                        //date_0_2015-01-01
                        endCal.add(Calendar.DATE, 1);//最后日期加一天，便于循环
                        while (startCal.before(endCal)) {
                            String date = String.format("%tF", startCal.getTime());
                            if (spotPlanDateList != null && !spotPlanDateList.isEmpty()) {
                                for (int k = 0; k < spotPlanDateList.size(); k++) {
                                    JSONObject spotDate = spotPlanDateList.getJSONObject(k);
                                    Date spotDay = DateUtil.getDate(spotDate.getString("day"));
                                    if (date.equals(DateUtil.getDateStr(spotDay))) {
                                        //点位信息
                                        String spot = spotDate.getString("spot");
                                        headData.add(getColItemObject(sheetId, startRow, dateColumns, spot));

                                        //配上则删除集合
                                        spotPlanDateList.remove(k);
                                        break;
                                    }
                                }
                            }
                            startCal.add(Calendar.DATE, 1);//开始日期加1
                            //列加+1
                            dateColumns++;
                        }
                    }
                }
            }
        }

        return headData;
    }



    /**
     * 查询全部的媒体记录
     *
     * @return
     */
    private JSONArray getAllMedia(int first, int size) {

        JSONArray spotArray;
        StringBuffer sql = new StringBuffer();
        sql.append("select id,meidaId,onlineTimeArange,boughtType,displayQuantity,orderPrice,retailPriceTotal,orderPriceTotal from saleContractSpot where contractId=").append(contractId);
        sql.append(" order by createdAt asc");
        QueryResult saleContractSpot = queryResult(sql.toString());
        if (saleContractSpot==null) return null;
        if (saleContractSpot.getCount()==0) return null;

        spotArray = saleContractSpot.getRecords();
        StringBuffer mediaIds = new StringBuffer();
        for (int i = 0; i < spotArray.size(); i++) {
            String mediaId = spotArray.getJSONObject(i).getString("meidaId");
            mediaIds.append(mediaId);
            if (i!=spotArray.size()-1){
                mediaIds.append(",");
            }
        }

        String spotField = spotConfig.getSql();
        sql = new StringBuffer();
        sql.append("select id").append(spotField).append(" from media ");
        sql.append(" where id in (").append(mediaIds).append(")");

        QueryResult mediaResult = queryResult(request, sql.toString());
        if (mediaResult!=null){
            JSONArray mediaArray = mediaResult.getRecords();
            for (int i = 0; i < spotArray.size(); i++) {
                JSONObject spotObj = spotArray.getJSONObject(i);
                String spotId =spotObj.getString("id");

                String mediaId=  spotObj.getString("meidaId");
                for (int j = 0; j < mediaArray.size(); j++) {
                    JSONObject mediaObj = mediaArray.getJSONObject(i);
                    String mediaObjId= mediaObj.getString("id");
                    if (mediaId.equals(mediaObjId)){
                        //增加媒体数据
                        spotObj.putAll(mediaObj);
                        //恢复spotId
                        spotObj.put("id", spotId);
                        break;
                    }
                }
            }
        }

        return spotArray;
    }

    /**
     * 查询排期的点位纪录
     * @return
     */
    private JSONArray getSpotDate(String spotId) {
        StringBuffer sql = new StringBuffer();
        sql.append("select id,day,spot from saleContractSpotDate where spotId=").append(spotId);
        QueryResult queryResult = queryResult(request, sql.toString());
        if (queryResult==null) return new JSONArray();
        if (queryResult.getCount()==0) return new JSONArray();
        return queryResult.getRecords();
    }
}