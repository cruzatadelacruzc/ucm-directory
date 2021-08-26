package cu.sld.ucmgt.directory.service.filter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.time.LocalDate;
import java.util.List;

/**
 * Filter class for {@link LocalDate} type attributes.
 *
 * @see RangeFilter
 */
public class LocalDateFilter extends RangeFilter<LocalDate> {

    private static final long serialVersionUID = 1L;

    public LocalDateFilter() {
    }

    /**
     * <p>Constructor for LocalDateFilter.</p>
     *
     * @param filter a {@link LocalDateFilter} object.
     */
    public LocalDateFilter(LocalDateFilter filter) {
        super(filter);
    }

    /** {@inheritDoc} */
    @Override
    public LocalDateFilter copy() {
        return new LocalDateFilter(this);
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE)
    public LocalDateFilter setGreaterThan(LocalDate greaterThan) {
        super.setGreaterThan(greaterThan);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE)
    public LocalDateFilter setLessThan(LocalDate lessThan) {
        super.setLessThan(lessThan);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE)
    public LocalDateFilter setGreaterThanOrEqual(LocalDate greaterThanOrEqual) {
        super.setGreaterThanOrEqual(greaterThanOrEqual);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE)
    public LocalDateFilter setLessThanOrEqual(LocalDate lessThanOrEqual) {
        super.setLessThanOrEqual(lessThanOrEqual);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE)
    public LocalDateFilter setEquals(LocalDate equals) {
        super.setEquals(equals);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE)
    public LocalDateFilter setNotEquals(LocalDate notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE)
    public LocalDateFilter setSpecified(Boolean specified) {
        super.setSpecified(specified);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE)
    public LocalDateFilter setIn(List<LocalDate> in) {
        super.setIn(in);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE)
    public LocalDateFilter setNotIn(List<LocalDate> notIn) {
        super.setNotIn(notIn);
        return this;
    }
}
