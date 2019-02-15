package com.kafka.action.hdfs_action;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.mapred.FsInput;
import org.apache.avro.reflect.ReflectData;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.alibaba.fastjson.parser.Feature;
import com.kafka.action.util.AvroSchemaUtil;
import com.kafka.action.util.ConvertDateType;

public class AvroToHdfs extends HashMap<String, Object> {

	private static final long serialVersionUID = -5203767970358787214L;

	/*
	 * @path Hdfs路径
	 * 
	 * @msg kafka接受的消息
	 * 
	 * @fs hdfs 系统
	 * 
	 * @init_createFile 是否第一次创建文件
	 */
	public static void avroSchema(Path path, ConsumerRecord<String, String> msg, FileSystem fs, int init_createFile)
			throws IOException {

		String tableschema = AvroSchemaUtil.getSchema(msg);
		Schema schema = new Schema.Parser().parse(tableschema);
		GenericRecord table = new GenericData.Record(schema);
		Map<String, Object> mapafter = AvroSchemaUtil.mapafter;
		Map<String, Object> mapbefore = AvroSchemaUtil.mapbefore;
		/**
		 * Hadoop 写入信息
		 */

		if (mapafter.size() > 0) {
			for (Entry<String, Object> entry : mapafter.entrySet()) {
				table.put(entry.getKey(), entry.getValue());
			}
		}

		if (mapbefore.size() > 0) {
			for (Entry<String, Object> entry : mapbefore.entrySet()) {
				table.put(entry.getKey(), entry.getValue());
			}
		}

		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
		DataFileWriter<GenericRecord> writer = new DataFileWriter(datumWriter).setCodec(CodecFactory.snappyCodec());

		DataFileWriter<GenericRecord> dataFileWriter = null;
		FSDataOutputStream outputStream = fs.append(path);

		if (init_createFile == 0) {
			dataFileWriter = writer.create(schema, outputStream);
			dataFileWriter.append(table);
		} else {
			dataFileWriter = writer.appendTo(new FsInput(path, fs.getConf()), outputStream);
			dataFileWriter.append(table);
		}
		writer.close();
		dataFileWriter.close();
		outputStream.close();
	}

}
