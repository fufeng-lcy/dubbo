/*
 * The MIT License (MIT)
 * ------------------------------------------------------------------
 * Copyright © 2019 Ramostear.All Rights Reserved.
 *
 * ProjectName: dubbo-parent
 * @Author : <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @date : 2020-09-18
 * @version : 1.0.0-RELEASE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.apache.simple.transport.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.simple.compress.Compressor;
import org.apache.simple.compress.CompressorFactory;
import org.apache.simple.constants.RpcConstant;
import org.apache.simple.exception.MagicNumberMatchException;
import org.apache.simple.protocol.Header;
import org.apache.simple.protocol.Response;
import org.apache.simple.serialization.Serialization;
import org.apache.simple.serialization.SerializationFactory;
import org.apache.simple.protocol.Message;
import org.apache.simple.protocol.Request;
import org.apache.simple.utils.CheckUtil;

import java.util.List;

/**
 * @program: dubbo-parent
 * @description: Rpc 消息解码器
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class RpcMessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext,
                          ByteBuf byteBuf, List<Object> list) throws Exception {
        // 如果本次可读取的字节小于头定义最小字节,暂时不读取
        if (byteBuf.readableBytes() < RpcConstant.HEAD_SIZE){
            return;
        }

        // 记录当前readIndex指针位置，方便重置
        byteBuf.markReaderIndex();
        // 读取head中的魔术信息
        final short magic = byteBuf.readShort();
        // 魔数和当前的版本不一致,抛出魔数不一致异常
        if (magic != RpcConstant.MAGIC){
            // 重置读索引
            byteBuf.resetReaderIndex();
            // 魔数不匹配异常
            throw new MagicNumberMatchException("magic number not match");
        }

        // 读取头信息中的所有字段
        // 版本号
        final byte version = byteBuf.readByte();
        // 扩展信息
        final byte extraInfo = byteBuf.readByte();
        // 消息ID
        final long msgId = byteBuf.readLong();
        // 消息长度
        final int length = byteBuf.readInt();

        // 构建请求消息对象
        Object body = null;
        // 检查是不是心跳消息,心跳消息没有消息体，无需读取
        if (!CheckUtil.isHeartBeat(extraInfo)){
            // 对于非心跳消息，没有积累到足够的消息内容也无需读取
            if (byteBuf.readableBytes() < length){
                // 重置读索引
                byteBuf.resetReaderIndex();
                return;
            }
            // 接受消息字节数据
            byte[] payload = new byte[length];
            byteBuf.readBytes(payload);

            // 根据扩展信息获取到对应的压缩格式和序列化格式
            final Serialization serialization =
                    SerializationFactory.get(extraInfo);
            final Compressor compressor =
                    CompressorFactory.get(extraInfo);

            if (CheckUtil.isRequest(extraInfo)) {
                // 将消息体解码构建一个Request对象
                body = serialization
                        .deSerialize(compressor.unCompress(payload), Request.class);
            }else {
                // 将消息体解码构建一个Response对象
                body = serialization
                        .deSerialize(compressor.unCompress(payload), Response.class);
            }
        }
        // 将上面的读取的数据封装成一个Head
        Header header = new Header(magic,version,extraInfo,msgId,length);
        // 构建一个message对象
        Message message = new Message(header,body);

        // 将消息添加到下一个处理handler
        list.add(message);
    }
}
