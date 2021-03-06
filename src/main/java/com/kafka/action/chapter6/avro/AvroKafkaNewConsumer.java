package com.kafka.action.chapter6.avro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.TopicPartition;
import org.apache.log4j.Logger;

public class AvroKafkaNewConsumer {

	private static final Logger LOG = Logger.getLogger(AvroKafkaNewConsumer.class);
	private static final int MSG_SIZE = 100;
	private static final int TIME_OUT = 100;
	private static final String TOPIC = "stock-quotation-avro";
	private static final String GROUPID = "test";
	private static final String CLIENTID = "test";

	
	private static final String BROKER_LIST = "192.168.1.70:9092,192.168.1.71:9092,192.168.1.72:9092,192.168.1.73:9092";
	private static final int AUTOCOMMITOFFSET = 0;

	private static Properties pops = null;

	private static KafkaConsumer<String, AvroStockQuotation> kafkaConsumerconsumer = null;

	static {
		// 1.构建用于实例化KafkaConsumer 的 properties 信息
		Properties pops = initProperties();
		// 2.初始化一个KafkaProducer
		kafkaConsumerconsumer = new KafkaConsumer<>(pops);
	}

	/* 初始化配置文件 */
	@SuppressWarnings("unused")
	private static Properties initProperties() {

		pops = new Properties();
		pops.put("bootstrap.servers", BROKER_LIST);
		pops.put("group.id", GROUPID);
		pops.put("client.id", CLIENTID);
		if (AUTOCOMMITOFFSET == 0) {
			pops.put("fetch.max.bytes", 1024);// 一次获取最大数据了为1K
			pops.put("enable.auto.commit", false); // 消费者偏移量管理，关闭自动提交
		}
		if (AUTOCOMMITOFFSET == 1) {
			pops.put("enable.auto.commit", true);// 消费者偏移量管理，设置自动提交
			pops.put("auto.commit.interval.ms", 1000); // 消费者偏移量管理，设置自动提交间隔
		}
		/* 设置自定义反序列化 */
		pops.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		pops.put("value.deserializer", "com.kafka.action.chapter6.avro.AvroDeserializer");

		return pops;
	}

	/* 订阅主题, 消费消息,自动提交偏移量 */
	@SuppressWarnings("unused")
	private static void subscribeTopicAuto(KafkaConsumer<String, AvroStockQuotation> consumer, String topic) {
		if (AUTOCOMMITOFFSET == 1) {
			consumer.subscribe(Arrays.asList(topic));
			ConsumerTopicMessage(consumer);
		} else {
			LOG.info("设置了手动提交，请检查配置信息！！！");
		}

	}

	/* 订阅主题, 消费消息,手动提交偏移量 */
	private static void subscribeTopicCustom1(KafkaConsumer<String, AvroStockQuotation> consumer, String topic) {
		if (AUTOCOMMITOFFSET == 0) {
			consumer.subscribe(Arrays.asList(topic), new ConsumerRebalanceListener() {
				@Override
				public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
					// TODO Auto-generated method stub
					consumer.commitAsync(); // 提交偏移量

				}

				@Override
				public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
					// TODO Auto-generated method stub
					long committedOffset = -1;
					for (TopicPartition topicPartition : partitions) {
						// 获取该分区的 偏移量
						committedOffset = consumer.committed(topicPartition).offset();
						// 重置偏移量到上次提交的偏移量下一个位置处开始消费
						consumer.seek(topicPartition, committedOffset);

					}
				}
			});

		} else {
			LOG.info("设置了自动提交，请检查配置信息！！！");
		}

		ConsumerTopicMessage(consumer);

	}

	private static void subscribeTopicCustom(KafkaConsumer<String, AvroStockQuotation> kafkaConsumerconsumer,
			String topic) {
		int minCommitSize = 10;// 至少需要处理10条再提交
		int icount = 0;// 消息计数器
		int icount1 = 0;// 消息计数器
		if (AUTOCOMMITOFFSET == 0) {
			kafkaConsumerconsumer.subscribe(Arrays.asList(topic));
			while (true) {
				try {
					ConsumerRecords<String, AvroStockQuotation> records = kafkaConsumerconsumer.poll(TIME_OUT);
					for (ConsumerRecord<String, AvroStockQuotation> record : records) {
						System.out.printf("消费的消息: partition = %d,offset = %d, key = %s ,value = %s%n",
								record.partition(), record.offset(), record.key(), record.value());
						icount++;
						icount1++;
					}
					if (icount >= minCommitSize) {
						System.out.println(icount1);
						kafkaConsumerconsumer.commitAsync(new OffsetCommitCallback() {

							@Override
							public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets,
									Exception exception) {
								// TODO Auto-generated method stub
								if (exception == null) {
									LOG.info("提交成功!!!");
								} else {
									LOG.error("提交失败!!!");
								}
							}
						});
						icount = 0;
					}

				} catch (Exception e) {
					// TODO: handle exception
					LOG.error("消费消息发生异常！！", e);
					break;
				}
				/*
				 * finally { consumer.close(); }
				 */
			}
		}
	}

	/* 订阅特定分区 */
	@SuppressWarnings("unused")
	private static void subscribeTopicPartition(KafkaConsumer<String, AvroStockQuotation> consumer, String topic,
			int... partitions) {
		ArrayList<TopicPartition> Topicpartitions = new ArrayList<TopicPartition>();
		for (int partitionId : partitions) {
			Topicpartitions.add(new TopicPartition(topic, partitionId));
		}

		consumer.assign(Topicpartitions);
		ConsumerTopicMessage(consumer);

	}

	/* 订阅主题, 消费消息,按时间戳消费消息 */
	private static void subscribeTopicTimestamp(KafkaConsumer<String, AvroStockQuotation> consumer, String topic,
			int... partitions) {
		ArrayList<TopicPartition> Topicpartitions = new ArrayList<TopicPartition>();
		for (int partitionId : partitions) {
			/* Topicpartitions.add(new TopicPartition(topic, partitionId)); */
			consumer.assign(Arrays.asList(new TopicPartition(topic, partitionId)));

			try {
				Map<TopicPartition, Long> timestampToSearch = new HashMap<TopicPartition, Long>();
				TopicPartition partition = new TopicPartition(TOPIC, partitionId);

				// 查询12小时之前的
				timestampToSearch.put(partition, (System.currentTimeMillis() - 72 * 360000 * 1000));
				// 会返回时间大于等于查找时间的第一个偏移量
				Map<TopicPartition, OffsetAndTimestamp> offSet = consumer.offsetsForTimes(timestampToSearch);
				OffsetAndTimestamp offsetAndTimestamp = null;

				for (Entry<TopicPartition, OffsetAndTimestamp> entry : offSet.entrySet()) {
					offsetAndTimestamp = entry.getValue();
					if (offsetAndTimestamp != null) {
						// 重置消费起始偏移量
						consumer.seek(partition, entry.getValue().offset());
					}
				}
				ConsumerTopicMessage(consumer);

			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	/* 获取消息 */
	private static void ConsumerTopicMessage(KafkaConsumer<String, AvroStockQuotation> consumer) {

		try {

			ConsumerRecords<String, AvroStockQuotation> records = consumer.poll(1000);
			for (ConsumerRecord<String, AvroStockQuotation> record : records) {
				System.out.printf("消费的消息: %n  partition = %d,offset = %d, key = %s , value = %s%n", record.partition(),
						record.offset(), record.key(), record.value());
			}

		} catch (Exception e) {
			// TODO: handle exception
			LOG.error("消费消息发生异常！！", e);
		}

		/*
		 * finally { consumer.close(); }
		 */

	}

	public static void main(String[] args) {
		/*
		 * for (int i = 0; i < 6; i++) { KafkaNewConsumer target = new
		 * KafkaNewConsumer(); target.subscribeTopicAuto(consumer, TOPIC); new
		 * Thread(target).start(); }
		 */

		AvroKafkaNewConsumer target = new AvroKafkaNewConsumer();
		/* target.subscribeTopicAuto(kafkaConsumerconsumer, TOPIC); */
		target.subscribeTopicCustom(kafkaConsumerconsumer, TOPIC);
		/* target.subscribeTopicTimestamp(kafkaConsumerconsumer,TOPIC,0); */
		kafkaConsumerconsumer.close();

	}
}
