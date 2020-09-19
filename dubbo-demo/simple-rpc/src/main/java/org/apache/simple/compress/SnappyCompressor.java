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
package org.apache.simple.compress;

import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.Objects;

/**
 * @program: dubbo-parent
 * @description: 基于 Snappy 压缩算法的实现
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class SnappyCompressor implements Compressor{

    /**
     *  基于snappy压缩算法压缩对应的byte数组
     * @param array 待压缩数组
     * @return 压缩后的byte 数组
     * @throws IOException io
     */
    @Override
    public byte[] compress(byte[] array) throws IOException {
        if (Objects.isNull(array)){
            return null;
        }
        return Snappy.compress(array);
    }

    /**
     *  基于snappy压缩算法解压对应的bute数组
     * @param array 待解压数组
     * @return 解压后的byte 数组
     * @throws IOException io
     */
    @Override
    public byte[] unCompress(byte[] array) throws IOException {
        if (Objects.isNull(array)){
            return null;
        }
        return Snappy.uncompress(array);
    }

}
