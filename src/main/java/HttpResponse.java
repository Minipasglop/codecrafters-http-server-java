import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class HttpResponse {

    byte[] status;
    byte[] headers;
    byte[] body;

}
