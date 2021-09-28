package cu.sld.ucmgt.directory.web.rest.util;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.MessageFormat;


public final class PaginationUtil {

    private static final String HEADER_X_TOTAL_COUNT = "X-Total-Count";
    private static final String HEADER_X_SORT = "X-Sort";
    private static final String HEADER_X_SIZE = "X-Size";
    private static final String HEADER_X_PAGE = "X-Page";
    private static final String HEADER_X_PAGEABLE = "X-Pageable";

    public PaginationUtil() {
    }

    public static <T> HttpHeaders generatePaginationHeaders(UriComponentsBuilder builder, Page<T> page) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_X_TOTAL_COUNT,Long.toString(page.getTotalElements()));
        headers.add(HEADER_X_SORT,page.getSort().toString());
        headers.add(HEADER_X_SIZE,Long.toString(page.getSize()));
        headers.add(HEADER_X_PAGE,Long.toString(page.getNumber()));
        headers.add(HEADER_X_PAGEABLE, String.valueOf(page.getPageable().isPaged()));

        int pageNumber = page.getNumber();
        int pageSize = page.getSize();
        StringBuilder link = new StringBuilder();
        if ( pageNumber < page.getTotalPages() ) {
            link.append(prepareLink(builder, pageNumber + 1 , pageSize, "next")).append(",");
        }

        if ( pageNumber > 0 ) {
            link.append(prepareLink(builder, pageNumber - 1 , pageSize, "prev")).append(",");
        }

        link.append(prepareLink(builder, page.getTotalPages() -1  , pageSize, "last"))
                .append(",")
                .append(prepareLink(builder, 0, pageSize, "first"));
        headers.add(HttpHeaders.LINK, link.toString());
        return headers;

    }

    private static String prepareLink(UriComponentsBuilder builder, int pageNumber, int pageSize, String relType) {
        return MessageFormat.format("<{0}>; rel\"{1}\"", builder.replaceQueryParam(
                        "page",
                        Integer.toString(pageNumber)
                    )
                    .replaceQueryParam(
                        "size",
                            Integer.toString(pageSize)
                    )
                    .toUriString()
                    .replace(",","%2C")
                    .replace(";","%3B")
             , relType
        );
    }
}
