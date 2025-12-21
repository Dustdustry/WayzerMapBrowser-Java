package mapbrowser.utils;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.serialization.*;

import java.io.*;
import java.util.*;

public class FormData{
    private final ObjectMap<String, Object> data = new ObjectMap<>();
    private final String boundary;

    public FormData(){
        byte[] bytes = new byte[8];
        new Random().nextBytes(bytes);
        this.boundary = "----WebKitFormBoundary" + Time.millis() + new String(Base64Coder.encode(bytes));
    }

    public void append(String key, Object value){
        data.put(key, value);
    }

    public String getContentType(){
        return "multipart/form-data; boundary=" + boundary;
    }

    public InputStream getStream(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream(Streams.defaultBufferSize);

        try(DataOutputStream writer = new DataOutputStream(baos)){
            // 遍历所有数据项
            data.each((key, value) -> {
                try{
                    // 写入boundary和字段头
                    writer.writeBytes("--" + boundary + "\r\n");
                    writer.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"");

                    if(value instanceof Fi file){
                        writer.writeBytes("; filename=\"" + file.nameWithoutExtension() + "\"\r\n");
                        writer.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
                        writer.flush();

                        try(InputStream fileStream = file.read()){
                            Streams.copy(fileStream, baos);
                        }
                    }else{
                        writer.writeBytes("\r\n\r\n");
                        writer.writeBytes(value.toString());
                    }

                    writer.writeBytes("\r\n");
                }catch(IOException e){
                    throw new RuntimeException(e);
                }
            });

            writer.writeBytes("--" + boundary + "--\r\n");
            writer.flush();
        }catch(IOException e){
            throw new RuntimeException("Failed to create form data", e);
        }

        return new ByteArrayInputStream(baos.toByteArray());
    }

    public void clear(){
        data.clear();
    }

    public int size(){
        return data.size;
    }

    public boolean contains(String key){
        return data.containsKey(key);
    }

    public Object remove(String key){
        return data.remove(key);
    }
}