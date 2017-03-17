package other.aakepi.bdfjfaackcpic.api.media;

import com.rkhd.platform.sdk.api.ApiSupport;
import com.rkhd.platform.sdk.http.Request;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import other.aakepi.bdfjfaackcpic.config.SpotField;
import other.aakepi.bdfjfaackcpic.util.DateUtil;
import other.aakepi.bdfjfaackcpic.util.DoubleUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 采购合同排期保存，继承查询对象，自动获得字段显示顺序
 * Created by yangyixin on 16/10/8.
 */
public class PurContractSpotSave extends PurContractSpotSearch implements ApiSupport {

    long spotDateBelongId;//点位表BelongId
    long mediaSpotDate888BelongID;  //媒体库点位表
    /**
     * 初始化
     *
     */
    protected void initParam() {
        super.initParam();
//        spotDateBelongId = getBelongId(allBelongs, "purContractSpotDate");
        spotDateBelongId=100018389; //Long.parseLong(Configuration4Belong.getInstance().getValue("purContractSpotDateBelongId"));
        mediaSpotDate888BelongID=100274711;// Long.parseLong(Configuration4Belong.getInstance().getValue("mediaSpotDate888BelongId"));
    }
    @Override
    public String execute(Request request, Long userId, Long tenantId) {
        initRequest(request);
        long start = System.currentTimeMillis();
        contractId = request.getParameter("id");
        startDate = DateUtil.getDate(request.getParameter("startDate"));
        endDate = DateUtil.getDate(request.getParameter("endDate"));
        String json = request.getParameter("json");

        JSONObject jsonResult = new JSONObject();

//        com.alibaba.fastjson.JSONArray cellArray = null;
        JSONArray cellArray = null;
        logger.debug("load start:" + (System.currentTimeMillis() - start) + " ms");
//        com.alibaba.fastjson.JSONObject obj1 = com.alibaba.fastjson.JSONObject.parseObject(json);

        JSONObject obj1 = JSONObject.fromObject(json);
        if (obj1.containsKey("cells")) {
            cellArray = obj1.getJSONArray("cells");
            //logger.debug("size:"+cellArray.size()+"value:" +cellArray.get(0));
        }
        logger.debug("load start obj:" + (System.currentTimeMillis() - start) + " ms");


        if (StringUtils.isBlank(json) || startDate == null || endDate == null || StringUtils.isBlank(contractId) || cellArray == null || cellArray.isEmpty()) {
            jsonResult.accumulate("message", "参数错误");
            jsonResult.accumulate("success", false);
            return jsonResult.toString();
        }
        //初始化合同
        contract = getContract(contractId);

        //合同不为空
        if (contract.isEmpty() || contract.containsKey("error_code")) {
            jsonResult.accumulate("message", "参数错误");
            jsonResult.accumulate("success", false);
            return jsonResult.toString();
        }
        //初始化参数
        initParam();

        HashMap<Integer, String> colTempMap = colTempMap();
        List<HashMap<String, String>> spotList = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> uuidMap = new HashMap<String, String>(cellArray.size());

        logger.debug("load title:" + (System.currentTimeMillis() - start) + " ms");
        //行号，spot的行号
        //解析全部数据
        for (int i = 0; i < cellArray.size(); i++) {

//            com.alibaba.fastjson.JSONObject cell = (com.alibaba.fastjson.JSONObject) cellArray.get(i);
           JSONObject cell = cellArray.getJSONObject(i);
            if (cell == null)
                continue;

            int row = cell.getInt("row");
            //从第四行开始
            if (row <= 3)
                continue;
            int col = cell.getInt("col");
            String json1 = cell.getString("json");
//            com.alibaba.fastjson.JSONObject jsonCell = com.alibaba.fastjson.JSONObject.parseObject(json1);
            JSONObject jsonCell = JSONObject.fromObject(json1);
            if (jsonCell != null) {
                HashMap<String, String> dataMap = new HashMap<String, String>();
                if (row >= spotList.size()) {
                    for (int j = spotList.size(); j <= row; j++) {
                        spotList.add(new HashMap<String, String>(colTempMap.size()));
                    }
                }
                dataMap = spotList.get(row);

//                String data = jsonCell.getString("data");
//                String value = jsonCell.getString("value");
//                String id = jsonCell.getString("id");
//                String cal = jsonCell.getString("cal");

                String data = jsonCell.containsKey("data")?jsonCell.getString("data"):"";
                String value = jsonCell.containsKey("value")?jsonCell.getString("value"):"";
                String id = jsonCell.containsKey("id")?jsonCell.getString("id"):"";
                String cal = jsonCell.containsKey("cal")?jsonCell.getString("cal"):"";

                String key = colTempMap.get(col);
                //包含指定的列
                if (colTempMap.containsKey(col)) {
                    if ("true".equals(cal)) {
                        data = value;
                    }
                    if ("uuid".equals(key)) {
                        if (StringUtils.isNotBlank(id)) {
                            //复制某一行，结果UUID也复制了,所以复制的uuid，设置为新增。
                            if (uuidMap.containsKey(id)) {
                                //
                                id = "";
                            } else {
                                uuidMap.put(id, id);
                            }
                            //内容＝ID
                            data = id;
                        }
                    }else if ("name".equals(key)){
                        //媒体名称
                        dataMap.put("mediaId",id);
                    }
                    dataMap.put(key, data);
                }

                logger.debug("row:" + row + ";col=" + col + ";key="+key+";data=" + data + ";value=" + value + ";id=" + id + ";all:" + cellArray.get(i));
//				logger.debug("load data :"+ i +" : "+ (System.currentTimeMillis() - start) + " ms");
            }
        }

        logger.debug("load data:" + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();


        //处理业务：

        int rows = 0;
        int delRow = 0;
        StringBuffer stringBuffer_msg = new StringBuffer();
        boolean hasSpotItem = false;
        for (int i = 0; i < spotList.size(); i++) {
            HashMap<String, String> dataMap = spotList.get(i);
            logger.debug("i:" + i + ";" + dataMap);

            String spotId =  dataMap.get("uuid");//行唯一编号
            String mediaId = dataMap.get("mediaId");//媒体ID
            String firstName = dataMap.get(spotConfig.getFistFieldName());

            //空行
            if (StringUtils.isBlank(spotId) && StringUtils.isBlank(firstName)&& StringUtils.isBlank(mediaId)) {
                continue;
            }

            //小计行不处理
            if (StringUtils.isNotBlank(spotId) && (firstName.indexOf("小计") >= 0 || firstName.indexOf("总计") >= 0)) {
                continue;
            }

            int currentRow = i;

            hasSpotItem = true;
            //点位纪录
            JSONArray spotPlanDateList = getSpotDate(spotId);
            Map<String,JSONObject> spotPlanMap=new HashMap<String, JSONObject>();
            if(spotPlanDateList!=null &&!spotPlanDateList.isEmpty()){
                //处理媒体库点位表
                for (int j = 0; j < spotPlanDateList.size(); j++) {
                    JSONObject spotDate = spotPlanDateList.getJSONObject(j);
                    Date spotDay = DateUtil.getDate(spotDate.getString("day"));
                    spotPlanMap.put(DateUtil.getDateStr(spotDay),spotDate);
                }
            }


           /* //媒体点位纪录
            JSONArray spotMediaDateList = getMediaSpotDate(mediaId);

            Map<String,JSONObject> mediaSpotMap=new HashMap<String, JSONObject>();
            if(spotMediaDateList!=null &&!spotMediaDateList.isEmpty()){
                //处理媒体库点位表
                for (int j = 0; j < spotMediaDateList.size(); j++) {
                    JSONObject spotDate = spotMediaDateList.getJSONObject(j);
                    Date spotDay = DateUtil.getDate(spotDate.getString("customItem1"));
                    mediaSpotMap.put(DateUtil.getDateStr(spotDay),spotDate);
                }
            }*/

            int spotNum = 0;

            //------------------------------------------
            if (startDate != null && endDate != null) {
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(startDate);
                Calendar endCal = Calendar.getInstance();
                endCal.setTime(endDate);
                //date_0_2015-01-01
                endCal.add(Calendar.DATE, 1);//最后日期加一天，便于循环
                boolean hasSpot = false;
                while (startCal.before(endCal)) {
                    String date = String.format("%tF", startCal.getTime());
                    startCal.add(Calendar.DATE, 1);//开始日期加1

//						String dateKey = "date_" + i + "_" + date;
                    String dateValue = dataMap.get(date);
                    double dateSpot = DoubleUtil.getValue(dateValue, 2, 0);

                    if (!hasSpot && StringUtils.isNotBlank(dateValue) && dateSpot!=0) {
                        hasSpot = true;
                        ++spotNum;
                    }
                    if (StringUtils.isBlank(dateValue) ||dateSpot==0) continue;
                    //处理外购采购点位记录
                    if(!spotPlanMap.isEmpty()&&spotPlanMap.containsKey(date)){
                        JSONObject spotDateJSon = spotPlanMap.get(date);
                        //设置新的点位 外购媒体的采购，可销售为0 。
                        spotDateJSon.put("spot", dateValue);
                        updateBelongs(spotDateJSon);
                        spotPlanMap.remove(date);
                    }/*else{
                        //在插入purContrantSpotDate的同时往媒体总库中插入输入。
                        JSONObject mediaSpotDate = new JSONObject();
                        mediaSpotDate.accumulate("customItem1",date);
                        mediaSpotDate.accumulate("spot",dateValue);
                        mediaSpotDate.accumulate("meidaID",mediaId);
                        mediaSpotDate.accumulate("comment","新采购媒体");
                        logger.info("新插入的mediaSpotDate：===" + mediaSpotDate);
                        createBelongs(mediaSpotDate888BelongID, mediaSpotDate);
                    }*/

            /*        for (int j = 0; j < spotPlanDateList.size(); j++) {
                        JSONObject spotDate = spotPlanDateList.getJSONObject(j);
                        Date spotDay = DateUtil.getDate(spotDate.getString("day"));
                        String spot = spotDate.getString("qty");
                        String spotDayStr=DateUtil.getDateStr(spotDay);
                        if (date.equals(spotDayStr)) {
                            //没有更新
                            if (!dateValue.equalsIgnoreCase(spot)) {
                                //设置新的点位
                                spotDate.put("qty",dateValue);
                                updateBelongs(spotDate);
                            }
                            spotNum += DoubleUtil.getValue(dateValue);
                            existsPurData=true;
                            //匹配上则删除集合
                            spotPlanDateList.remove(j);
                            break;
                        }

                    }

                    //不存在，新增
                    if (!existsPurData){
                        JSONObject spotDate = new JSONObject();
                        spotDate.accumulate("day",date);
                        spotDate.accumulate("qty",dateSpot);
                        spotDate.accumulate("spotId",spotId);
                        spotDate.accumulate("contractId",contractId);
                        spotDate.accumulate("mediaId",mediaId);
                        spotDate.accumulate("dimDepart",contract.getString("dimDepart"));
                        createBelongs(spotDateBelongId,spotDate);
                        //在插入purContrantSpotDate的同时往媒体总库中插入输入。
                        JSONObject mediaSpotDate = new JSONObject();
                        mediaSpotDate.accumulate("customItem1",date);
                        mediaSpotDate.accumulate("spot","0");
                        mediaSpotDate.accumulate("meidaID",mediaId);
                        mediaSpotDate.accumulate("comment","新采购媒体");
                        createBelongs(mediaSpotDate888BelongID,mediaSpotDate);
                        spotNum += DoubleUtil.getValue(dateValue);
                    }*/
                }

                //删除本次修改的点位

                /*for (int j = 0; j < spotPlanMap..size() ; j++) {
                    JSONObject spotDate = spotPlanDateList.getJSONObject(j);
                    long spotDateId = spotDate.getLong("id");
                    deleteBelongs(spotDateId);
                }*/
                Iterator it = spotPlanMap.keySet().iterator();
                while(it.hasNext()) {
                    JSONObject spotDate = (JSONObject)it.next();
                    long spotDateId = spotDate.getLong("id");
                    deleteBelongs(spotDateId);
                }

                if (!hasSpot) {
                    stringBuffer_msg.append("第" + currentRow + "行广告位，必须有点位纪录\n");
//						return getErrorResponse(currentRow, "必须有点位纪录！");
                }

                //自動計算总刊例数、刊例总价、折后总价
                JSONArray spots= getSpot(spotId);

                if (spots!=null&&spots.size()>0){
                    JSONObject spot = spots.getJSONObject(0);
                    Double orderPrice = spot.getDouble("disPrice");
                    spot.put("id",spotId);
                    //日期
                    JSONArray spotDateArray = getSpotDate(spotId);
                    if (spotDateArray!=null&&spotDateArray.size()>0){
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                        try {
                            spot.put("startAt",format.parse(spotDateArray.getJSONObject(0).getString("day")).getTime());
                            spot.put("endAt",format.parse(spotDateArray.getJSONObject(spotDateArray.size()-1).getString("day")).getTime());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    spot.put("qty",spotNum);
                    spot.put("purAmount",DoubleUtil.mul(spotNum,orderPrice));

                    //自动计算
                    updateBelongs(spot);
                }
            }
        }

        if (!hasSpotItem) {
            stringBuffer_msg.append("没有一条点位记录，请检查！");
        }

        if (stringBuffer_msg.length() > 0) {
            jsonResult.accumulate("message", stringBuffer_msg.toString());
            jsonResult.accumulate("success", false);
            return jsonResult.toString();
        }


        logger.debug("save data:" + (System.currentTimeMillis() - start) + " ms");

        jsonResult.accumulate("message", "保存成功");
        jsonResult.accumulate("success", true);

        return jsonResult.toString();
    }


    /**
     * 获得列配置位置信息
     *
     * @return 列位置：字段名称
     */
    private HashMap<Integer, String> colTempMap() {
        //初始化全部列
        HashMap<Integer, String> colTempMap = new HashMap<Integer, String>(400);
        int dateColumns = 0;
        colTempMap.put(dateColumns++, "uuid");
        List<SpotField> spotFieldList = spotConfig.getSpotFielList();
        for (int i = 0; i < spotFieldList.size(); i++) {
            SpotField spotField = spotFieldList.get(i);
            String fieldName = spotField.getEn();
            //增加列信息
            colTempMap.put(dateColumns++, fieldName);

            //增加日期配置信息
            boolean renderDateColumn = fieldName.equalsIgnoreCase(spotConfig.getShowSpotFieldName());
            if (renderDateColumn) {
                if (startDate != null && endDate != null) {
                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(startDate);
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(endDate);
                    //date_0_2015-01-01
                    endCal.add(Calendar.DATE, 1);//最后日期加一天，便于循环
                    while (startCal.before(endCal)) {
                        String date = DateUtil.getDateStr(startCal.getTime());//String.format("%tF", startCal.getTime());
                        //logger.debug(date+";"+dateColumns+";"+ SheetUtil.indexToColumn(dateColumns));
                        colTempMap.put(dateColumns, date);
                        dateColumns++;
                        startCal.add(Calendar.DATE, 1);//开始日期加1
                    }
                }
            }
        }

        return colTempMap;
    }

    /**
     * 查询排期的点位纪录
     * @return
     */
    private JSONArray getSpotDate(String spotId) {
        StringBuffer sql = new StringBuffer();
        sql.append("select id,day,qty from purContractSpotDate where spotId=").append(spotId);
        sql.append(" order by day ");
        JSONArray result=queryAllResult( sql.toString());
        logger.info("PurContractSportSave.getSpotDate("+spotId+"):="+result);
        return result;
    }

    private JSONArray getSpot(String spotId) {
        StringBuffer sql = new StringBuffer();
        sql.append("select id,disPrice from purContractSpot where id=").append(spotId);
        JSONArray result=queryAllResult(sql.toString());
        logger.info("PurContractSportSave.getSpot(" + spotId + "):=" + result);
        return result;
    }

    /**
     * 查询排期总表点评数据
     * @return
     */
    private JSONArray getMediaSpotDate(long  mediaId,  Long beginLong ,  Long endLong ) {
        StringBuffer sql = new StringBuffer();
        sql.append("select id,customItem1,spot,comment from mediaSpotDate888 where meidaID=").append(mediaId);
        sql.append(" and day >= ").append(beginLong).append(" and day <= ").append(endLong);
        JSONArray result = queryAllResult(sql.toString());
        logger.info("PurContractSportSave.getMediaSpotDate(" + mediaId + "):=" + result);
        return result;

    }

}
