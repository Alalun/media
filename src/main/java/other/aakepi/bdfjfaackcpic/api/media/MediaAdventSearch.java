package other.aakepi.bdfjfaackcpic.api.media;

import com.rkhd.platform.sdk.api.ApiSupport;
import com.rkhd.platform.sdk.http.Request;
import com.rkhd.platform.sdk.test.tool.TestCustomizeApiTool;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import other.aakepi.bdfjfaackcpic.api.BaseApiSupport;
import other.aakepi.bdfjfaackcpic.api.QueryResult;
import other.aakepi.bdfjfaackcpic.util.DateUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 媒体临期查询
 * Created by YuJinliang on 16/10/6.
 */
public class MediaAdventSearch extends BaseApiSupport implements ApiSupport {


    @Override
    public String execute(Request request, Long userId, Long tenantId) {
        int first = 0;
        int size = 20;

        Map<String, Object> returnMap = new HashMap<String, Object>();

        String op = request.getParameter("op");

        JSONObject condition = new JSONObject();
        condition.put("time", request.getParameter("time"));
        condition.put("customer", request.getParameter("customer"));
        condition.put("dept", request.getParameter("dept"));
        condition.put("contract", request.getParameter("contract"));
        condition.put("keyword", request.getParameter("keyword"));

        System.out.println("condition-------" + condition.toString());

        QueryResult result = null;
        String start = request.getParameter("start");
        if (StringUtils.isNotBlank(start)) {
            int startInt = NumberUtils.toInt(start);
            first = startInt * size;
        }
        //合同纪录
        if ("pur".equals(op)) {
            result = getPurMediaAdventList(condition, first, size);
        } else if ("sales".equals(op)) {
            //媒体查询
            result = getSalesMediaAdventList(condition, first, size);
        }
        JSONArray records = new JSONArray();
        if (result != null) {
            records = result.getRecords();
            if (records != null)
                System.out.println("records-----------" + records.toString());
        }
        return records.toString();
    }

    /**
     * 查询全部采购临期媒体记录
     *
     * @param condition
     * @return
     */
    private QueryResult getPurMediaAdventList(JSONObject condition, int first, int size) {

        StringBuffer sql = new StringBuffer();
        JSONArray records = new JSONArray();
        JSONObject record = null;
        JSONObject result = new JSONObject();

        //1 先查询合同
        sql.append("select id,supplier,name,code,operator from purchasingContract where id>0");
        if (isNotNull(condition, "contract")) {
            sql.append(" and (name like '%" + condition.getString("contract") + "%' or code like '%" + condition.getString("contract") + "%')");
        }
        JSONArray contractArray = queryResultArray(sql.toString());
        if (contractArray != null && contractArray.size() > 0) {
            JSONObject contract = null;
            for (int i = 0; i < contractArray.size(); i++) {
                contract = contractArray.getJSONObject(i);
                if (contract == null) continue;
                sql = new StringBuffer();
                sql.append("select id,mediaId,disAmount,startAt,endAt from purContractSpot ");
                sql.append(" where contractId = " + contract.getString("id"));
                if (isNotNull(condition, "time")) {
                    if ("25".equals(condition.getString("time"))) {
                        sql.append(" and endAt >= " + DateUtil.getTodayStart().getTime());
                        sql.append(" and endAt <= " + DateUtil.getOneMonthEnd().getTime());
                    } else if ("26".equals(condition.getString("time"))) {
                        sql.append(" and endAt >= " + DateUtil.getTwoMonthStart().getTime());
                        sql.append(" and endAt <=" + DateUtil.getTwoMonthEnd().getTime());
                    } else if ("27".equals(condition.getString("time"))) {
                        sql.append(" and endAt >= " + DateUtil.getThreeMonthStart().getTime());
                        sql.append(" and endAt <= " + DateUtil.getThreeMonthEnd().getTime());
                    }else if("all".equals(condition.getString("time"))){
                        sql.append(" and endAt >= " + DateUtil.getTodayStart().getTime());
                        sql.append(" and endAt <= " + DateUtil.getThreeMonthEnd().getTime());
                    }
                }

                //2 查询排期明细
                JSONArray spotArray = queryResultArray(sql.toString());
                JSONObject spot = null;
                if (spotArray != null && spotArray.size() > 0) {
                    for (int j = 0; j < spotArray.size(); j++) {
                        spot = spotArray.getJSONObject(j);
                        if (spot == null) continue;
                        record = new JSONObject();
                        record.put("contract", contract.getString("code"));
                        record.put("operator", contract.getString("operator"));
                        record.put("startAt", spot.getString("startAt"));
                        record.put("endAt", spot.getString("endAt"));
                        record.put("color", getRowColor(spot.getString("endAt")));
                        record.put("disAmount", spot.getString("disAmount"));

                        sql = new StringBuffer();
                        sql.append("select id,code,name,address from media ");
                        sql.append(" where id = " + spot.getString("mediaId"));
                        JSONArray mediaArray = queryResultArray(sql.toString());
                        JSONObject media;
                        if (mediaArray != null && mediaArray.size() > 0) {
                            media = mediaArray.getJSONObject(0);
                            if (media != null) {
                                record.put("mediaCode", media.getString("code"));
                                record.put("mediaName", media.getString("name"));
                            }
                        }

                        records.add(record);

                    }
                }

            }
            result.put("records", records);
            result.put("totalSize", records.size());
            result.put("count", records.size());
        }

        return (QueryResult) JSONObject.toBean(result, QueryResult.class);
    }

    private String getRowColor(String endAt) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if (StringUtils.isNotBlank(endAt)){
            try {
                Date endDate = sdf.parse(endAt);
                if (DateUtil.isBefore(endDate,DateUtil.getOneMonthEnd())){
                    return "red";
                }else if(DateUtil.isBefore(endDate,DateUtil.getTwoMonthEnd())){
                    return "orange";
                }else if(DateUtil.isBefore(endDate,DateUtil.getThreeMonthEnd())){
                    return "blue";
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private boolean isNotNull(JSONObject condition, String time) {
        return condition != null && condition.containsKey(time) && StringUtils.isNotBlank(time);
    }

    /**
     * 查询全部销售临期媒体记录
     *
     * @return
     */
    private QueryResult getSalesMediaAdventList(JSONObject condition, int first, int size) {

        StringBuffer sql = new StringBuffer();
        sql.append("select id,mediaId,contractId,address,endDate from salesContractSpot ");
        sql.append(" limit ").append(first).append(",").append(size);

        return queryResult(sql.toString());
    }


    public static void main(String[] args) {
        TestCustomizeApiTool testTriggerTool = new TestCustomizeApiTool();
        MediaAdventSearch api = new MediaAdventSearch();
        testTriggerTool.test("search-adventmedia", api);
    }


}
