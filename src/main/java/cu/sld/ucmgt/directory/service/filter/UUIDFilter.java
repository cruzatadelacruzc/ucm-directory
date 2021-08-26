package cu.sld.ucmgt.directory.service.filter;

import java.util.UUID;

/**
 * Filter class for {@link UUID} type attributes.
 *
 * @see Filter
 */
public class UUIDFilter extends Filter<UUID> {
    private static final long serialVersionUID = 1L;

    public UUIDFilter() {}

    /**
     * <p>Constructor for UUIDFilter.</p>
     *
     * @param filter a {@link UUIDFilter} object.
     */
    public UUIDFilter(final UUIDFilter filter) {
        super(filter);
    }

    /**
     * <p>copy.</p>
     *
     * @return a {@link UUIDFilter} object.
     */
    @Override
    public UUIDFilter copy() {
        return new UUIDFilter(this);
    }
}
