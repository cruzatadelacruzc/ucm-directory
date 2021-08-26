package cu.sld.ucmgt.directory.service.filter;

/**
 * Filter class for {@link java.lang.Boolean} type attributes.
 *
 * @see RangeFilter
 */
public class BooleanFilter extends Filter<Boolean> {

    private static final long serialVersionUID = 1L;


    public BooleanFilter() {
    }

    /**
     * <p>Constructor for BooleanFilter.</p>
     *
     * @param filter a {@link BooleanFilter} object.
     */
    public BooleanFilter(BooleanFilter filter) {
        super(filter);
    }

    /** {@inheritDoc} */
    @Override
    public BooleanFilter copy() {
        return new BooleanFilter(this);
    }
}
