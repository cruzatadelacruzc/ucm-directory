package cu.sld.ucmgt.directory.service.filter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.time.Instant;
import java.util.List;

/**
 * Filter class for {@link Instant} type attributes.
 *
 * @see RangeFilter
 */
public class InstantFilter extends RangeFilter<Instant> {

    private static final long serialVersionUID = 1L;

    public InstantFilter() {
    }

    /**
     * <p>Constructor for InstantFilter.</p>
     *
     * @param filter a {@link InstantFilter} object.
     */
    public InstantFilter(InstantFilter filter) {
        super(filter);
    }

    /** {@inheritDoc} */
    @Override
    public InstantFilter copy() {
        return new InstantFilter(this);
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE_TIME)
    public InstantFilter setGreaterThan(Instant greaterThan) {
        super.setGreaterThan(greaterThan);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE_TIME)
    public InstantFilter setLessThan(Instant lessThan) {
        super.setLessThan(lessThan);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE_TIME)
    public InstantFilter setGreaterThanOrEqual(Instant greaterThanOrEqual) {
        super.setGreaterThanOrEqual(greaterThanOrEqual);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE_TIME)
    public InstantFilter setLessThanOrEqual(Instant lessThanOrEqual) {
        super.setLessThanOrEqual(lessThanOrEqual);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE_TIME)
    public InstantFilter setEquals(Instant equals) {
        super.setEquals(equals);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE_TIME)
    public InstantFilter setNotEquals(Instant notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE_TIME)
    public InstantFilter setSpecified(Boolean specified) {
        super.setSpecified(specified);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE_TIME)
    public InstantFilter setIn(List<Instant> in) {
        super.setIn(in);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    @DateTimeFormat(iso = ISO.DATE_TIME)
    public InstantFilter setNotIn(List<Instant> notIn) {
        super.setNotIn(notIn);
        return this;
    }
}
