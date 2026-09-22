package com.tzh.baselib.util.voice;

import android.media.AudioFormat;

import com.tzh.baselib.util.LogUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class PcmToMp3Util {
    private static final String TAG = "PcmToMp3Util";

    public static String path;


    private short[] transferByte2Short(byte[] data, int readBytes) {
        // byte[] 转 short[]，数组长度缩减一半
        int shortLen = readBytes / 2;
        // 将byte[]数组装如ByteBuffer缓冲区
        ByteBuffer byteBuffer = ByteBuffer.wrap(data, 0, readBytes);
        // 将ByteBuffer转成小端并获取shortBuffer
        ShortBuffer shortBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] shortData = new short[shortLen];
        shortBuffer.get(shortData, 0, shortLen);
        return shortData;
    }


    /**
     * @param src    待转换文件路径
     * @param target 目标文件路径
     * @throws IOException 抛出异常
     *                     <p>
     *                     这个转换 后的大小 跟 源文件一致，暂不使用
     */
    public static File convertAudioFiles(File src, File target) throws IOException {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(target);

        //计算长度
        byte[] buf = new byte[1024 * 4];
        int size = fis.read(buf);
        int PCMSize = 0;
        while (size != -1) {
            PCMSize += size;
            size = fis.read(buf);
        }
        fis.close();

        //填入参数，比特率等等。这里用的是16位单声道 8000 hz
        WaveHeader header = new WaveHeader();
        //长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.setFileLength(PCMSize + (44 - 8));
        header.setFmtHdrLeth(16);
        header.setBitsPerSample((short) 16) ;
        header.setChannels((short) 1);
        header.setFormatTag((short) 0x0001);
        header.setSamplesPerSec(44100);//正常速度是8000，这里写成了16000，速度加快一倍
        header.setBlockAlign((short) (header.getChannels() * header.getBitsPerSample() / 8)) ;
        header.setAvgBytesPerSec(header.getBlockAlign() * header.getSamplesPerSec());
        header.setDataHdrLeth(PCMSize);

        byte[] h = header.getHeader();

        assert h.length == 44; //WAV标准，头部应该是44字节
        //write header
        fos.write(h, 0, h.length);
        //write data stream
        fis = new FileInputStream(src);
        size = fis.read(buf);
        while (size != -1) {
            fos.write(buf, 0, size);
            size = fis.read(buf);
        }
        fis.close();
        fos.close();
        System.out.println("PCM Convert to MP3 OK!");
        return target;
    }
}
