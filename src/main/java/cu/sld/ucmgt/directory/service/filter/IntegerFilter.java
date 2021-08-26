package cu.sld.ucmgt.directory.service.filter;

/**
 * Filter class for {@link java.lang.Integer} type attributes.
 *
 * @see RangeFilter
 */
public class IntegerFilter extends RangeFilter<Integer> {

    private static final long serialVersionUID = 1L;

    public IntegerFilter() {
    }

    /**
     * <p>Constructor for IntegerFilter.</p>
     *
     * @param filter a {@link IntegerFilter} object.
     */
    public IntegerFilter(IntegerFilter filter) {
        super(filter);
    }

    /** {@inheritDoc} */
    @Override
    public IntegerFilter copy() {
        return new IntegerFilter(this);
    }
}
