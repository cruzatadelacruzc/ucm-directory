package cu.sld.ucmgt.directory.service.filter;

/**
 * Filter class for {@link java.lang.Double} type attributes.
 *
 * @see RangeFilter
 */
public class DoubleFilter extends RangeFilter<Double> {

private static final long serialVersionUID = 1L;

    /**
     * <p>Constructor for DoubleFilter.</p>
     *
     * @param filter a {@link DoubleFilter} object.
     */
    public DoubleFilter(RangeFilter<Double> filter) {
        super(filter);
    }

    /** {@inheritDoc} */
    @Override
    public DoubleFilter copy() {
        return new DoubleFilter(this);
    }
}
