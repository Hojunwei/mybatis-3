/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

/**
 *
 * 通用的handler处理
 * 从头开始寻找${XXX}表达式，并提交给 handler 进行处理
 *
 * @author Clinton Begin
 */
public class GenericTokenParser {

    /**
     * 开始的token字符串
     */
    private final String openToken;

    /**
     * 结束的token字符串
     */
    private final String closeToken;

    /**
     * 替换token字符串的处理接口
     */
    private final TokenHandler handler;

    public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
        this.openToken = openToken;
        this.closeToken = closeToken;
        this.handler = handler;
    }

    /**
     *
     * 查找token字符串，调用TokenHandler，返回替换后的字符串
     *
     * @param text
     * @return
     */
    public String parse(String text) {
        // 判断为空
        if (text == null || text.isEmpty()) {
            return "";
        }
        // search open token 寻找开始的token位置
        int start = text.indexOf(openToken);
        if (start == -1) { // 不存在，直接返回
            return text;
        }
        char[] src = text.toCharArray();
        int offset = 0;

        // 结果
        final StringBuilder builder = new StringBuilder();
        // openToken 和 closeToken之间的表达式
        StringBuilder expression = null;
        while (start > -1) {
            if (start > 0 && src[start - 1] == '\\') {
                // this open token is escaped. remove the backslash and continue.
                builder.append(src, offset, start - offset - 1).append(openToken);
                offset = start + openToken.length();
            } else {
                // found open token. let's search close token.
                // 创建/重置 expression 对象
                if (expression == null) {
                    expression = new StringBuilder();
                } else {
                    expression.setLength(0);
                }
                // 添加 offset 和 openToken 之间的内容，添加到 builder 中
                builder.append(src, offset, start - offset);
                // 修改 offset
                offset = start + openToken.length();
                // 寻找结束的 closeToken 的位置
                int end = text.indexOf(closeToken, offset);
                while (end > -1) {
                    if (end > offset && src[end - 1] == '\\') {
                        // this close token is escaped. remove the backslash and continue.
                        expression.append(src, offset, end - offset - 1).append(closeToken);
                        offset = end + closeToken.length();
                        end = text.indexOf(closeToken, offset);
                    } else {
                        // 添加${XXX}之间的内容到expression
                        expression.append(src, offset, end - offset);
                        // 修改offset
                        offset = end + closeToken.length();
                        break;
                    }
                }
                if (end == -1) {
                    // close token was not found.
                    builder.append(src, start, src.length - start);
                    offset = src.length;
                } else {
                    // expression的值获取真正的值
                    builder.append(handler.handleToken(expression.toString()));
                    offset = end + closeToken.length();
                }
            }
            // 继续，寻找下一个 openToken 的位置
            start = text.indexOf(openToken, offset);
        }
        // 拼接剩余的部分
        if (offset < src.length) {
            builder.append(src, offset, src.length - offset);
        }
        return builder.toString();
    }
}
