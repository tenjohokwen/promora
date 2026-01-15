package com.softropic.promora.utils.sql;

public class DeleteQuery extends SqlQuery {
    @Override
    protected String getQueryType() {
        return "DELETE";
    }
}
