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
package org.apache.simple.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.simple.compress.Compressor;
import org.apache.simple.compress.CompressorFactory;
import org.apache.simple.protocol.Header;
import org.apache.simple.protocol.Response;
import org.apache.simple.serialization.Serialization;
import org.apache.simple.serialization.SerializationFactory;
import org.apache.simple.protocol.Message;
import org.apache.simple.utils.CheckUtil;

/**
 * @program: dubbo-parent
 * @description: Rpc 消息编码器
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class RpcMessageEncoder extends MessageToByteEncoder<Message<Response>> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext,
                          Message message, ByteBuf byteBuf) throws Exception {
        final Header header = message.getHeader();
        // 按顺序依次写入header相关信息
        byteBuf.writeShort(header.getMagic());
        byteBuf.writeByte(header.getVersion());
        byteBuf.writeByte(header.getExtraInfo());
        byteBuf.writeLong(header.getMessageId());

        // 如果消息是心跳消息，那么就写入长度为0
        if (CheckUtil.isHeartBeat(header.getExtraInfo())){
            byteBuf.writeInt(0);
            return;
        }

        // 获取消息体信息
        final Object payload = message.getPayload();

        // 按照指定的序列化和压缩算法处理数据
        final Serialization serialization =
                SerializationFactory.get(header.getExtraInfo());
        final Compressor compressor =
                CompressorFactory.get(header.getExtraInfo());
        final byte[] payloadByte =
                compressor.compress(serialization.serialize(payload));
        // 写出消息体长度
        byteBuf.writeInt(payloadByte.length);
        // 写出消息内容
        byteBuf.writeBytes(payloadByte);
    }
}
