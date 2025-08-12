package me.dingtou.options.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.dingtou.options.model.EarningsCalendar;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.Date;
import java.util.List;

/**
 * 财报日历Mapper
 */
public interface EarningsCalendarDAO extends BaseMapper<EarningsCalendar> {

    /**
     * 根据股票代码查询财报日历
     * 
     * @param symbol 股票代码
     * @return 财报日历列表
     */
    @Select("SELECT * FROM earnings_calendar WHERE symbol = #{symbol}")
    List<EarningsCalendar> selectBySymbol(@Param("symbol") String symbol);

    /**
     * 根据日期查询财报日历
     * 
     * @param date 指定日期
     * @return 财报日历列表
     */
    @Select("SELECT * FROM earnings_calendar WHERE earnings_date = #{date}")
    List<EarningsCalendar> selectByDate(@Param("date") Date date);

    /**
     * 根据日期范围查询财报日历
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 财报日历列表
     */
    @Select("SELECT * FROM earnings_calendar WHERE earnings_date >= #{startDate} AND earnings_date <= #{endDate}")
    List<EarningsCalendar> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 根据股票代码和财报日期范围删除财报日历
     * 
     * @param symbol 股票代码
     * @param earningsDate 财报日期
     * @return 影响行数
     */
    @Delete("DELETE FROM earnings_calendar WHERE symbol = #{symbol} AND earnings_date >= DATE_SUB(#{earningsDate}, INTERVAL 1 MONTH) AND earnings_date <= DATE_ADD(#{earningsDate}, INTERVAL 1 MONTH)")
    int deleteBySymbolAndEarningsDateRange(@Param("symbol") String symbol, @Param("earningsDate") Date earningsDate);

    /**
     * 插入或替换财报日历
     * 
     * @param earningsCalendar 财报日历对象
     * @return 影响行数
     */
    @Insert("INSERT INTO earnings_calendar (symbol, earnings_date, name, market_cap, fiscal_quarter_ending, eps_forecast, no_of_ests, last_year_rpt_dt, last_year_eps, time, created_time, updated_time) " +
            "VALUES (#{symbol}, #{earningsDate}, #{name}, #{marketCap}, #{fiscalQuarterEnding}, #{epsForecast}, #{noOfEsts}, #{lastYearRptDt}, #{lastYearEps}, #{time}, #{createdTime}, #{updatedTime}) "  +
            "ON DUPLICATE KEY UPDATE " +
            "name = VALUES(name), " +
            "market_cap = VALUES(market_cap), " +
            "fiscal_quarter_ending = VALUES(fiscal_quarter_ending), " +
            "eps_forecast = VALUES(eps_forecast), " +
            "no_of_ests = VALUES(no_of_ests), " +
            "last_year_rpt_dt = VALUES(last_year_rpt_dt), " +
            "last_year_eps = VALUES(last_year_eps), " +
            "time = VALUES(time), " +
            "updated_time = VALUES(updated_time)")
    int insertReplace(EarningsCalendar earningsCalendar);
}