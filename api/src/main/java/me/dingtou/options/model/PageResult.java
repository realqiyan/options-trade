package me.dingtou.options.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果
 *
 * @author qiyan
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> data;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 总页数
     */
    private Integer pages;

    public PageResult() {
    }

    public PageResult(List<T> data, Long total, Integer page, Integer size) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = (int) Math.ceil((double) total / size);
    }

    /**
     * 创建分页结果
     *
     * @param data  数据列表
     * @param total 总记录数
     * @param page  当前页码
     * @param size  每页大小
     * @param <T>   数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> data, Long total, Integer page, Integer size) {
        return new PageResult<>(data, total, page, size);
    }
}