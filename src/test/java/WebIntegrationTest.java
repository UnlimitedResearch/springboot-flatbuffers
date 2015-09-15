import com.google.flatbuffers.FlatBufferBuilder;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import lombok.extern.slf4j.Slf4j;
import org.horiga.study.springboot.flatbuffers.Application;
import org.horiga.study.springboot.flatbuffers.FlatBuffersHttpMessageConverter;
import org.horiga.study.springboot.flatbuffers.protocol.messages.Me;
import org.horiga.study.springboot.flatbuffers.protocol.messages.Token;
import org.horiga.study.springboot.flatbuffers.protocol.messages.UserAnswer;
import org.horiga.study.springboot.flatbuffers.util.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@org.springframework.boot.test.WebIntegrationTest("server.port=18080")
public class WebIntegrationTest {

	@Autowired
	WebApplicationContext wac;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

	@Autowired
	FlatBuffersHttpMessageConverter flatBuffersHttpMessageConverter;

	@After
	public void tearDown() {}

	@Test
	public void test_flatbuffers_simple() throws Exception {

		ByteBuffer in_bb = me();
		Utils.hex(in_bb);

		AsyncHttpClient hc = new AsyncHttpClient(
				new AsyncHttpClientConfig.Builder().setRequestTimeout(3000).build());
		ListenableFuture<Response> f = hc.preparePost("http://localhost:18080/api")
				.addHeader("Content-Type", "application/x-fb")
				.addHeader("X-FBS-MessageId", "1")
				.setBody(in_bb.array())
				.setBodyEncoding("UTF-8")
				.execute();
		Response res = f.get(3000, TimeUnit.MILLISECONDS);

		log.info("res.contentType:{}", res.getContentType());
		log.info("res.status: {}", res.getStatusCode());

		ByteBuffer res_bb = res.getResponseBodyAsByteBuffer();

		log.info("RESPONSE >>>>>>>>>>>>");
		Utils.hex(res_bb);
		log.info("RESPONSE <<<<<<<<<<<<");

		UserAnswer answer = UserAnswer.getRootAsUserAnswer(res_bb);

		log.info("answer.displayName:{}", answer.displayName());
		log.info("answer.mid:{}", answer.mid());

		hc.close();
	}

	private static ByteBuffer me() throws Exception {

		FlatBufferBuilder fbb = new FlatBufferBuilder(0);

		int accessTokenOffset = fbb.createString("ThisIsAccessToken");

		Token.startToken(fbb);
		Token.addId(fbb, 123);
		Token.addAccessToken(fbb, accessTokenOffset);
		Token.addCreated(fbb, System.currentTimeMillis());
		int tokenOffset = Token.endToken(fbb);
		log.info("token.offset={}", tokenOffset);

		Me.startMe(fbb);
		Me.addToken(fbb, tokenOffset);
		int meOffset = Me.endMe(fbb);
		fbb.finish(meOffset);

		return ByteBuffer.wrap(fbb.sizedByteArray());
	}


}
