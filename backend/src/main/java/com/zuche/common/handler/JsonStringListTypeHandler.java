package com.zuche.common.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * JSON 数组字符串 ⇄ List&lt;String&gt;
 *
 * <p>评价图片在库里是一列 TEXT，存 {@code ["/images/cars/car-1.svg"]}。
 * 直接映射到 {@code List<String>} 会因类型不匹配拿到 null（MyBatis 不会报错，只会静默丢数据），
 * 所以显式挂一个 TypeHandler。</p>
 *
 * <p>解析失败一律返回空集合而不是抛异常：一条脏数据不应该让整个评价列表打不开。</p>
 */
@MappedTypes(List.class)
public class JsonStringListTypeHandler extends BaseTypeHandler<List<String>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {
    };

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter,
                                    JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("图片列表序列化失败", e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<String> parse(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = json.trim();
        // 兼容早期用逗号分隔的写法，避免历史数据直接变成空列表
        if (!trimmed.startsWith("[")) {
            return List.of(trimmed.split("\\s*,\\s*"));
        }
        try {
            return MAPPER.readValue(trimmed, TYPE);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
