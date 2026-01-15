package com.softropic.promora.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DbCleaner {

    @Autowired
    protected TransactionTemplate template;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    //Let tests determine if this is needed in before or after methods
    // e.g. It cannot be used as @BeforeEach for methods that load sql statements
    // If you want test data to persist after a test, you cannot also use @AfterEach
    public void cleanDb() {
        template.execute(status ->  {
            jdbcTemplate.execute("delete from main.booking_passenger");
            jdbcTemplate.execute("delete from main.booking");
            jdbcTemplate.execute("delete from main.schedule_template");
            jdbcTemplate.execute("delete from main.schedule");
            jdbcTemplate.execute("delete from main.bus");
            jdbcTemplate.execute("delete from main.city");
            jdbcTemplate.execute("delete from main.operator");
            jdbcTemplate.execute("delete from main.passenger");
            jdbcTemplate.execute("delete from main.route");
            jdbcTemplate.execute("delete from main.ticket");
            jdbcTemplate.execute("delete from main.transactions");
            jdbcTemplate.execute("delete from main.seat");
            jdbcTemplate.execute("delete from main.seat_reservation");
            jdbcTemplate.execute("delete from main.template_override");
            jdbcTemplate.execute("delete from main.template_scheduled_till");
            jdbcTemplate.execute("delete from main.blackout_date");
            jdbcTemplate.execute("delete from main.persistent_token");
            jdbcTemplate.execute("delete from main.user_authority");
            jdbcTemplate.execute("delete from main.audit_log");
            jdbcTemplate.execute("delete from main.user_addresses");
            jdbcTemplate.execute("delete from main.user");
            jdbcTemplate.execute("delete from main.authority");
            //jdbcTemplate.execute("delete from main.inventory");
            //jdbcTemplate.execute("delete from main.inventory_log");
            //jdbcTemplate.execute("delete from main.payment");
            //jdbcTemplate.execute("delete from main.mobile_pay_data");
            //jdbcTemplate.execute("delete from main.delivery_order_item_ids");
            jdbcTemplate.execute("delete from main.envelope_entity_recipients");
            jdbcTemplate.execute("delete from main.envelope_entity");
            jdbcTemplate.execute("delete from main.sec");
            jdbcTemplate.execute("delete from main.account");
            jdbcTemplate.execute("delete from main.ledger_entries");
            jdbcTemplate.execute("delete from main.voucher");
            jdbcTemplate.execute("delete from main.payment_transaction");

            return 0;
        });
    }

}
