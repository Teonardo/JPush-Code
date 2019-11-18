package com.ruoyi.project.app.JPush;

import cn.jiguang.common.ClientConfig;
import cn.jiguang.common.resp.APIConnectionException;
import cn.jiguang.common.resp.APIRequestException;
import cn.jpush.api.JPushClient;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Message;
import cn.jpush.api.push.model.Options;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.IosAlert;
import cn.jpush.api.push.model.notification.Notification;
import com.ruoyi.common.constant.AppPlatformType;
import com.ruoyi.common.constant.JPushBusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.config.PropertiesConfig;
import com.ruoyi.project.app.JPush.domain.JPushBusiness;
import com.ruoyi.project.app.JPush.mapper.JPushBusinessMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JPushTools {
	static Logger logger = LoggerFactory.getLogger(JPushTools.class);
    private static final String MASTER_SECRET = PropertiesConfig.JPUSH_MASTERSECRET;
    private static final String APP_KEY = PropertiesConfig.JPUSH_APPKEY;

	// 极光推送日志mapper
	private static JPushBusinessMapper jPushBusinessMapper = SpringUtils.getBean(JPushBusinessMapper.class);

	private  static Boolean apnsProduction;

	@Value("${JPush.ApnsProduction}")
	private void setApnsProduction(Boolean bol){
         apnsProduction = bol;
	}





	/**
	 * 测试tag推送
	 * @param args
	 */
	public static void main(String[] args) {
		String str = "65d9ae20-17af-43eb-9799-e880cff8c574";
		//sendPushMsgBytag("65d9ae20-17af-43eb-9799-e880cff8c574",MD5.bit16(str).toLowerCase(),"测试天天云教育","ceshi",3,"ererrrr");
	}



    /**
     * @Author  luobin
     * @Description 极光推送消息通知方法
     * @Date 2019/8/13 15:39
     * @param alias 设置推送的目标别名list
     * @param title 标题
     * @param content 内容
     * @param typeId 业务类型id
     * @param dataId 业务主键id
     * @return void
     **/
 	public static void sendPushMsg(List<String> alias,String title,String content,String typeId,String dataId) {
 		//初始化极光client
	    JPushClient jpushClient = new JPushClient(MASTER_SECRET, APP_KEY, null, ClientConfig.getInstance());
		String contentP = "";
		if(JPushBusinessType.contains(typeId)){
			JPushBusinessType jPushBusinessType = Enum.valueOf(JPushBusinessType.class,typeId);
			switch(jPushBusinessType){
				//随机抽取专家通知
				case RANDOM_EXTRACT_NOTICE:
					title = jPushBusinessType.getTitle();
					contentP = jPushBusinessType.getContent();
					break;
				//评标签到通知（专家）
				case BIDEVAL_EXPERT_SIGN_NOTICE:
					title = jPushBusinessType.getTitle();
					contentP = jPushBusinessType.getContent();
					break;
				//评标签到通知（监督人）
				case BIDEVAL_MONITOR_SIGN_NOTICE:
					title = jPushBusinessType.getTitle();
					contentP = jPushBusinessType.getContent();
					break;
				//回避审核通过通知
				case PRESENT_AVOID_PASS:
					title = jPushBusinessType.getTitle();
					contentP = jPushBusinessType.getContent();
					break;
				//回避审核不通过通知
				case PRESENT_AVOID_UNPASS:
					title = jPushBusinessType.getTitle();
					contentP = jPushBusinessType.getContent()+content;
					break;
			}
		}

		List<JPushBusiness> jPushBusinesses = new ArrayList<>();
		for(String ex:alias) {
			JPushBusiness jPushBusiness = new JPushBusiness();
			jPushBusiness.setTypeId(typeId);
			jPushBusiness.setDataId(dataId);
			jPushBusiness.setTargetId(ex);
			jPushBusiness.setTitle(title);
			jPushBusiness.setContent(contentP);
			jPushBusinesses.add(jPushBusiness);
		}
		//批量新增
		jPushBusinessMapper.insertJPushBatch(jPushBusinesses);
		String targetIds = StringUtils.join(alias,",");
		Integer  noticeId =jPushBusinesses.get(0).getId();



		// For push, all you need do is to build PushPayload object.
		for(AppPlatformType appPlatformType:AppPlatformType.values()) {
			PushPayload payload = buildPushMsg(alias,noticeId, title, contentP, typeId, dataId, appPlatformType.getCode());
			//PushPayload payload = buildPushObject_all_all_alert();
			try {
				logger.info("===========app平台:"+appPlatformType.getCode()+"推送log===========");
				logger.info(appPlatformType.getCode()+",payload:" + payload);
				PushResult result = jpushClient.sendPush(payload);
				logger.info(appPlatformType.getCode()+",sendPushMsg:" + result.toString());
			} catch (APIConnectionException e) {
				// Connection error, should retry laterde
				logger.info(appPlatformType.getCode()+",userId:"+targetIds+",sendPushMsg1:" + e.toString());
			} catch (APIRequestException e) {
				e.printStackTrace();
				logger.info(appPlatformType.getCode()+",userId:"+targetIds+",sendPushMsg2:" + e.toString());
			}
		}
    }

    public static PushPayload buildPushObject_all_all_alert(){
 		 return PushPayload.alertAll("签到通知");
	}

	public static PushPayload buildPushMsg(List<String> alias,Integer noticeId,String title,String content,String typeId,String dataId,String platFormType) {
		//String objectStr="{\"content\":\""+contentP+"\",\"typeId\":\""+typeId+"\",\"dataId\":\""+dataId+"\"}";

		if(platFormType.equals("android")){
			return buildPushMsgSingleAndorid(alias,noticeId,title,content,typeId,dataId);
		} else if(platFormType.equals("ios")){
			return buildPushMsgSingleIOS(alias,noticeId,title,content,typeId,dataId);
		} else{
			return null;
		}
	}



	/**
	 * 单人推送安卓
     * @param alias 推送对象（用户id）
     * @param title 通知标题
     * @param content 通知内容
     * @param typeId 业务类型id
     * @param dataId 业务主键id
	 * @return
	 */
	public static PushPayload buildPushMsgSingleAndorid(List<String> alias,Integer noticeId,String title,String content,String typeId,String dataId) {
		String objectStr="{\"content\":\""+content+"\",\"typeId\":\""+typeId+"\",\"dataId\":\""+dataId+"\"}";

		Map<String, String> extra = new HashMap<>();
		extra.put("typeId",typeId);
		extra.put("dataId",dataId);
		extra.put("noticeId",noticeId+"");
		return PushPayload.newBuilder()
                //设置APP 平台
				.setPlatform(Platform.android())
                //设置 别名
				.setAudience(Audience.alias(alias))
				.setOptions(Options.newBuilder().setApnsProduction(apnsProduction).build())
				//.setNotification(Notification.alert(title))
				.setNotification(Notification.android(content,title,extra))
				.setMessage(Message.newBuilder()
						.setContentType("text")
						.setMsgContent(objectStr)
						.setTitle(title)
						.addExtra("typeId", typeId)
						.addExtra("dataId", dataId)
						.addExtra("noticeId", noticeId)
						.build())
				.build();
	}

	/**
	 * 单人推送IOS
	 * @param alias 推送对象（用户id）
	 * @param title 通知标题
	 * @param content 通知内容
	 * @param typeId 业务类型id
	 * @param dataId 业务主键id
	 * @return
	 */
	public static PushPayload buildPushMsgSingleIOS(List<String> alias,Integer noticeId,String title,String content,String typeId,String dataId) {
		String objectStr="{\"content\":\""+content+"\",\"typeId\":\""+typeId+"\",\"dataId\":\""+dataId+"\"}";
		Map<String, String> extra = new HashMap<>();
		extra.put("typeId",typeId);
		extra.put("dataId",dataId);
		extra.put("noticeId",noticeId+"");
        //设置ios 标题 内容
        IosAlert alert1 = IosAlert.newBuilder()
                .setTitleAndBody(title, null, content)
                .setActionLocKey("PLAY")
                .build();
		return PushPayload.newBuilder()
				//设置APP 平台
				.setPlatform(Platform.ios())
				//设置 别名
				.setAudience(Audience.alias(alias))
				.setOptions(Options.newBuilder().setApnsProduction(apnsProduction).build())
				.setNotification(Notification.ios(alert1,extra))
                //.setNotification(Notification.alert(title))
				.setMessage(Message.newBuilder()
						.setContentType("text")
						.setMsgContent(objectStr)
						.setTitle(title)
						.addExtra("typeId", typeId)
						.addExtra("dataId", dataId)
						.addExtra("noticeId", noticeId)
						.build())
				.build();
	}
}
