package cu.sld.ucmgt.directory.service.filter;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Filter class for {@link ZonedDateTime} type attributes.
 *
 * @see RangeFilter
 */
public class ZonedDateTimeFilter extends RangeFilter<ZonedDateTime> {
    private static final long serialVersionUID = 1L;

    public ZonedDateTimeFilter() {
    }

    /**
     * <p>Constructor for RangeFilter.</p>
     *
     * @param filter a {@link RangeFilter} object.
     */
    public ZonedDateTimeFilter(ZonedDateTimeFilter filter) {
        super(filter);
    }

    /** {@inheritDoc} */
    @Override
    public ZonedDateTimeFilter copy() {
        return new ZonedDateTimeFilter(this);
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public RangeFilter<ZonedDateTime> setGreaterThan(ZonedDateTime greaterThan) {
        super.setGreaterThan(greaterThan);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public RangeFilter<ZonedDateTime> setLessThan(ZonedDateTime lessThan) {
        super.setLessThan(lessThan);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public RangeFilter<ZonedDateTime> setGreaterThanOrEqual(ZonedDateTime greaterThanOrEqual) {
        super.setGreaterThanOrEqual(greaterThanOrEqual);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public RangeFilter<ZonedDateTime> setLessThanOrEqual(ZonedDateTime lessThanOrEqual) {
        super.setLessThanOrEqual(lessThanOrEqual);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public Filter<ZonedDateTime> setEquals(ZonedDateTime equals) {
        super.setEquals(equals);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public Filter<ZonedDateTime> setNotEquals(ZonedDateTime notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public Filter<ZonedDateTime> setSpecified(Boolean specified) {
        super.setSpecified(specified);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public Filter<ZonedDateTime> setIn(List<ZonedDateTime> in) {
        super.setIn(in);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public Filter<ZonedDateTime> setNotIn(List<ZonedDateTime> notIn) {
        super.setNotIn(notIn);
        return this;
    }
}
