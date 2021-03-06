package org.crawler.parserdriver;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.LineReader;
import org.crawler.util.DocumentWritable;

public class ParserRecordReader extends RecordReader<Text, DocumentWritable> {
	
	//private static final Log LOG = LogFactory.getLog(ParserRecordReader.class); 
	
    private LineReader in; //输入流
    private boolean more = true; //提示后续还有没有数据
    
    private Text key = null; 
    private DocumentWritable value = null;
    
    //这三个是保存当前读取到位置（即文件中位置）
    private long start; 
    private long pos; 
    private long end; 
    
    /**
	 * 初始化函数
	 */
	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		FileSplit inputSplit = (FileSplit)split;
		start = inputSplit.getStart();//得到此分片的开头位置
		end = start + inputSplit.getLength();//得到此分片的结束位置
		
		final Path file = inputSplit.getPath();
		//打开文件
		FileSystem fs = file.getFileSystem(context.getConfiguration());
		FSDataInputStream fileIn = fs.open(inputSplit.getPath());
		
		//将文件指针移动到当前分片，因为每次默认打开文件时，其指针指向开头
		fileIn.seek(start);
		
		in = new LineReader(fileIn,context.getConfiguration());
		if(start != 0){
			//如果这不是第一个分片，那么假设第一个分片0-4，那么第4个位置被读取，则需要跳过4，否则会产生读入错误因为你回头又去读之前读过的地方
			start += in.readLine(new Text(), 0, maxBytesToConsume(start));
		}
		pos = start;
	}
	
	private int maxBytesToConsume(long pos){
		return (int)Math.min(Integer.MAX_VALUE, end - pos);
	}
	
	/**
	 * 下一组值
	 */
	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		if(null == key){
			key = new Text();
		}
		if(null == value){
			value = new DocumentWritable();
		}
		Text nowline = new Text();//保存当前行内容
		int readsize = in.readLine(nowline);
		//更新当前读取到的位置
		pos += readsize;
		//如果pos值大于等于end，说明此分片已经读取完毕
		if(pos >= end){
			more = false;
			return false;
		}
		
		if(0 == readsize){
			key = null;
			value = null;
			more = false;//此处说明已经读取到文件末尾
			return false;
		}
		System.out.println("nowline:"+nowline.toString());
		String[] keyandvalue = nowline.toString().split("\t");
		//排除第一行
		if(keyandvalue[0].endsWith("\"CITING\"")){
			readsize = in.readLine(nowline);
			//更新当前读取到位置
			pos += readsize;
			if(0 == readsize){
				more = false;
				return false;
			}
			//重新划分
			keyandvalue = nowline.toString().split("\t");
		}
		System.out.println("LENGTH:" + keyandvalue.length);
		//得到key和value
		StringBuffer doc = new StringBuffer("");
		for(int i=1;i<keyandvalue.length-3;i++){
			doc.append(keyandvalue[i]);
		}
		System.out.println("DOC:" + doc.toString());
		key.set(keyandvalue[0]);
		value.setDocument(doc.toString());
		value.setRedirectFrom(keyandvalue[keyandvalue.length-3]);
		value.setMetaFollow(Boolean.getBoolean(keyandvalue[keyandvalue.length-2]));
		value.setMetaIndex(Boolean.getBoolean(keyandvalue[keyandvalue.length-1]));		
		return true;
	} 
   
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		if(null != in){
			in.close();
		}
	}
	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return key;
	}
	@Override
	public DocumentWritable getCurrentValue() throws IOException,
			InterruptedException {
		// TODO Auto-generated method stub
		return value;
	}
	@Override
	public float getProgress() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		if(false == more || end == start){
			return 0f;
		}else{
			return Math.min(1.0f, (pos - start)/(end - start));
		}
	}
	
}
