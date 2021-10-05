package cu.sld.ucmgt.directory.service.filter;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Filter class for {@link LocalDateTime} type attributes.
 *
 * @see RangeFilter
 */
public class LocalDateTimeFilter extends RangeFilter<LocalDateTime> {

    private static final long serialVersionUID = 1L;

    public LocalDateTimeFilter() {
    }

    /**
     * <p>Constructor for LocalDateTimeFilter.</p>
     *
     * @param filter a {@link LocalDateTimeFilter} object.
     */
    public LocalDateTimeFilter(LocalDateTimeFilter filter) {
        super(filter);
    }

    /** {@inheritDoc} */
    @Override
    public LocalDateTimeFilter copy() {
        return new LocalDateTimeFilter(this);
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTimeFilter setGreaterThan(LocalDateTime greaterThan) {
        super.setGreaterThan(greaterThan);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTimeFilter setLessThan(LocalDateTime lessThan) {
        super.setLessThan(lessThan);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTimeFilter setGreaterThanOrEqual(LocalDateTime greaterThanOrEqual) {
        super.setGreaterThanOrEqual(greaterThanOrEqual);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTimeFilter setLessThanOrEqual(LocalDateTime lessThanOrEqual) {
        super.setLessThanOrEqual(lessThanOrEqual);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTimeFilter setEquals(LocalDateTime equals) {
        super.setEquals(equals);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTimeFilter setNotEquals(LocalDateTime notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTimeFilter setSpecified(Boolean specified) {
        super.setSpecified(specified);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTimeFilter setIn(List<LocalDateTime> in) {
        super.setIn(in);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTimeFilter setNotIn(List<LocalDateTime> notIn) {
        super.setNotIn(notIn);
        return this;
    }
}
