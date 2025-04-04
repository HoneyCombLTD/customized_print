package io.mosip.print.test.util;

import io.mosip.print.constant.UinCardType;
import io.mosip.print.core.http.ResponseWrapper;
import io.mosip.print.dto.ErrorDTO;
import io.mosip.print.dto.SignatureResponseDto;
import io.mosip.print.exception.ApisResourceAccessException;
import io.mosip.print.exception.PDFGeneratorException;
import io.mosip.print.exception.PDFSignatureException;
import io.mosip.print.service.PrintRestClientService;
import io.mosip.print.service.impl.UinCardGeneratorImpl;
import io.mosip.print.spi.PDFGenerator;
import io.mosip.print.test.TestBootApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TestBootApplication.class)
@RunWith(SpringRunner.class)
public class UinCardGeneratorImplTest {

	@Mock
	private PDFGenerator pdfGenerator;

	@InjectMocks
	private UinCardGeneratorImpl cardGeneratorImpl;

	@Mock
	private Environment env;

	@Mock
	private PrintRestClientService<Object> restClientService;

	@Before
	public void setUp() {
		when(env.getProperty("mosip.print.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ReflectionTestUtils.setField(cardGeneratorImpl, "lowerLeftX", 73);
		ReflectionTestUtils.setField(cardGeneratorImpl, "lowerLeftY", 100);
		ReflectionTestUtils.setField(cardGeneratorImpl, "upperRightX", 300);
		ReflectionTestUtils.setField(cardGeneratorImpl, "upperRightY", 300);
		ReflectionTestUtils.setField(cardGeneratorImpl, "reason", "signing");

	}

	@Test
	public void testCardGenerationSuccess() throws IOException, ApisResourceAccessException {
		ClassLoader classLoader = getClass().getClassLoader();
		String inputFile = classLoader.getResource("csshtml.html").getFile();
		InputStream is = new FileInputStream(inputFile);

		byte[] buffer = new byte[8192];
		int bytesRead;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		while ((bytesRead = is.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}

		Mockito.when(pdfGenerator.generate(is)).thenReturn(outputStream);
		ResponseWrapper<SignatureResponseDto> responseWrapper = new ResponseWrapper<>();
		SignatureResponseDto signatureResponseDto = new SignatureResponseDto();
		signatureResponseDto.setData(buffer.toString());
		responseWrapper.setResponse(signatureResponseDto);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any(), any(MediaType.class)))
				.thenReturn(responseWrapper);
		byte[] bos = (byte[]) cardGeneratorImpl.generateUinCard(is, UinCardType.PDF, null);

		String outputPath = System.getProperty("user.dir");
		String fileSepetator = System.getProperty("file.separator");
		File OutPutPdfFile = new File(outputPath + fileSepetator + "csshtml.pdf");
		FileOutputStream op = new FileOutputStream(OutPutPdfFile);
		op.write(bos);
		op.flush();
		assertTrue(OutPutPdfFile.exists());
		if (op != null) {
			op.close();
		}
		OutPutPdfFile.delete();
	}

	@Test(expected = PDFGeneratorException.class)
	public void testPdfGeneratorException() throws IOException, ApisResourceAccessException {
		ClassLoader classLoader = getClass().getClassLoader();
		String inputFileName = classLoader.getResource("emptyFile.html").getFile();
		File inputFile = new File(inputFileName);
		InputStream inputStream = new FileInputStream(inputFile);
		PDFGeneratorException e = new PDFGeneratorException(null, null);
		Mockito.doThrow(e).when(pdfGenerator).generate(inputStream);
		cardGeneratorImpl.generateUinCard(inputStream, UinCardType.PDF, null);
	}

	@Test(expected = PDFSignatureException.class)
	public void testPDFSignatureException() throws IOException, ApisResourceAccessException {
		ClassLoader classLoader = getClass().getClassLoader();
		String inputFile = classLoader.getResource("csshtml.html").getFile();
		InputStream is = new FileInputStream(inputFile);

		byte[] buffer = new byte[8192];
		int bytesRead;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		while ((bytesRead = is.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}
		ApisResourceAccessException e = new ApisResourceAccessException(null, null);
		Mockito.doThrow(e).when(restClientService).postApi(any(), any(), any(), any(), any(), any(MediaType.class));

		Mockito.when(pdfGenerator.generate(is)).thenReturn(outputStream);

		cardGeneratorImpl.generateUinCard(is, UinCardType.PDF, null);
	}

	@Test(expected = PDFSignatureException.class)
	public void testCardGenerationFailure() throws IOException, ApisResourceAccessException {
		ClassLoader classLoader = getClass().getClassLoader();
		String inputFile = classLoader.getResource("csshtml.html").getFile();
		InputStream is = new FileInputStream(inputFile);

		byte[] buffer = new byte[8192];
		int bytesRead;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		while ((bytesRead = is.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}

		Mockito.when(pdfGenerator.generate(is)).thenReturn(outputStream);
		ResponseWrapper<SignatureResponseDto> responseWrapper = new ResponseWrapper<>();
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		ErrorDTO error = new ErrorDTO();
		error.setErrorCode("KER-001");
		error.setMessage("error in digital signature");
		errors.add(error);
		responseWrapper.setErrors(errors);

		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any(), any(MediaType.class)))
				.thenReturn(responseWrapper);
		cardGeneratorImpl.generateUinCard(is, UinCardType.PDF, null);
	}
}
