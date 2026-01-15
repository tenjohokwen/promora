package com.softropic.promora.utils.sql;

public class InsertQuery extends SqlQuery {
    @Override
    protected String getQueryType() {
        return "INSERT";
    }
}
