package cu.sld.ucmgt.directory.service.filter;

/**
 * Filter class for {@link java.lang.Long} type attributes.
 *
 * @see RangeFilter
 */
public class LongFilter extends RangeFilter<Long> {

    private static final long serialVersionUID = 1L;

    public LongFilter() {
    }

    /**
     * <p>Constructor for LongFilter.</p>
     *
     * @param filter a {@link LongFilter} object.
     */
    public LongFilter(LongFilter filter) {
        super(filter);
    }

    /**{@inheritDoc} */
    @Override
    public LongFilter copy() {
        return new LongFilter(this);
    }
}
