package com.nodestory.controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.apache.poi.ss.formula.functions.T;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.axet.wget.WGet;
import com.nodestory.Main;
import com.nodestory.commons.Constants;
import com.nodestory.service.NaverWebtoonSelectService;
import com.nodestory.utils.AlertSupport;
import com.nodestory.utils.ButtonCell;
import com.nodestory.utils.ExcelReader;
import com.nodestory.utils.TextImageConvert;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

public class NaverEventController extends ListCell<T> implements Initializable {

	public MainController root;

	public void init(MainController mainController) {
		root = mainController;
	}

	@FXML
	public TextField codeField;

	@FXML
	public TextField titleField;

	@FXML
	public TextField sEp;

	@FXML
	public TextField dEp;

	@FXML
	public TextArea log;

	@FXML
	public CheckBox folderCheck;

	@FXML
	public CheckBox bestCommentCheck;

	@FXML
	public CheckBox jobListCheck;

	@FXML
	public CheckBox systemCheck;

	@FXML
	public Button downloadBtn;

	@FXML
	public Button jobListAddBtn;

	@FXML
	public ListView<String> jobWebtoonList;

	// 리스트뷰에 보여질 맵 정보
	public Map<String, Object> jobMap = new LinkedHashMap<String, Object>();

	// 리스트뷰에 보여질 데이터 리스트
	public ObservableList<String> listItems = FXCollections.observableArrayList();

	// 임시로 저장하기 위한 리스트
	public List<Map<String, Object>> fakeList = new ArrayList<Map<String, Object>>();

	public boolean commentFlag = false;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// 데이터 리스트를 동적으로 삽입하여 작업리스트에 보여준다.
		jobWebtoonList.setItems(listItems);

		// 작업리스트에 추가한 웹툰을 버튼형태로 만들어 삽입한다.
		jobWebtoonList.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> list) {
				return new ButtonCell(fakeList, jobWebtoonList, downloadBtn);
			}
		});
	}

	@FXML
	public void systemShutDown() {

		if (systemCheck.isSelected()) {

			AlertSupport alert = new AlertSupport(
					"이 기능을 사용하면 웹툰 다운로드 완료 시 시스템(컴퓨터)를 자동종료 합니다. 많은 편수의 웹툰을 다운로드 받을 때 유용합니다. 이 기능을 사용 하시겠습니까?");
			int count = alert.alertConfirm();
			if (count > 0) {
				systemCheck.setSelected(true);
			} else {
				systemCheck.setSelected(false);
			}
		}

	}

	@FXML
	public void jobListConfirm() {
		if (jobListCheck.isSelected()) {
			jobListAddBtn.setDisable(false);
			jobWebtoonList.setDisable(false);
			downloadBtn.setDisable(true);
			downloadBtn.setText("전체 다운로드");
		} else {

			if (fakeList.size() > 0) {
				AlertSupport alert = new AlertSupport("예약리스트를 비워주세요.");
				int count = alert.alertConfirm();
				if (count > 0) {
					jobListCheck.setSelected(true);
				} else {
					jobListCheck.setSelected(true);
				}
			} else {
				jobListAddBtn.setDisable(true);
				jobWebtoonList.setDisable(true);
				downloadBtn.setDisable(false);
				downloadBtn.setText("단일 다운로드");
			}
		}
	}

	@FXML
	public void commentConfirm() {
		if (bestCommentCheck.isSelected()) {
			AlertSupport alert = new AlertSupport("베스트 댓글을 추가 하시겠습니까?\n배댓추가시 다운로드 속도가 조금 느려지며, 배댓 개행처리가 안되므로 긴글은 짤리게됩니다. \n\n(이 기능은 현재 베타버전입니다)");
			int count = alert.alertConfirm();
			if (count > 0) {
				// code
				commentFlag = true;
			} else {
				bestCommentCheck.setSelected(false);
			}
		} else {
			commentFlag = false;
		}
	}

	@FXML
	public void resetdBtn() {
		// 모든 항목을 클리어 한다.
		codeField.clear();
		titleField.clear();
		sEp.clear();
		dEp.clear();
		folderCheck.setSelected(false);
		bestCommentCheck.setSelected(false);
		jobWebtoonList.getItems().clear();
		jobWebtoonList.setDisable(true);
		fakeList.clear();
		jobListCheck.setSelected(false);

		jobListAddBtn.setDisable(true);
		downloadBtn.setText("단일 다운로드");
	}

	@FXML
	public void addJobList() {

		// 리스트 사이즈가 0보다 크면
		if (fakeList.size() > 0) {

			boolean flag = false;

			// 실제 데이터를 담아둘 맵 정보
			Map<String, Object> fakeMap = new LinkedHashMap<String, Object>();
			fakeMap.put("webtoonTitle", titleField.getText());
			fakeMap.put("webtoonCode", codeField.getText());
			fakeMap.put("webtoonStartEp", sEp.getText());
			fakeMap.put("webtoonEndEp", dEp.getText());

			// 1. fakeList에 저장된 퉵툰코드와, 등록하고자 하는 웹툰코드가 있는지 체크한다.
			for (int i = 0; i < fakeList.size(); i++) {
				Map<String, Object> fakeData = fakeList.get(i);
				if (fakeData.get("webtoonCode").equals(codeField.getText())) {
					AlertSupport alert = new AlertSupport(Constants.AlertInfoMessage.DUPLICATED_WEBTOON);
					alert.alertInfoMsg();
					flag = false;
					break;
				} else {
					flag = true;
				}
			}

			if (flag) {
				// 리스트뷰에 보여질 제목 데이터
				jobMap.put("webtoonTitle", titleField.getText());
				jobMap.put("sEp", sEp.getText());
				jobMap.put("dEp", dEp.getText());
				listItems.add(String.valueOf(jobMap.get("webtoonTitle") + " ("
						+ String.valueOf(jobMap.get("sEp") + " ~ " + String.valueOf(jobMap.get("dEp") + ")"))));

				// 리스트 추가
				fakeList.add(fakeMap);
			}

		} else {

			if (sEp.getText().equals("") || dEp.getText().equals("")) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						AlertSupport alert = new AlertSupport("시작편수나, 종료편수는 필수값입니다.\n웹툰을 선택하세요.");
						alert.alertInfoMsg();
					}
				});
			} else {

				// 작업리스트에 최소 1개이상 추가 된다면 버튼 활성화
				downloadBtn.setDisable(false);

				// 최초 입력시 실행
				jobMap.put("webtoonTitle", titleField.getText());
				jobMap.put("sEp", sEp.getText());
				jobMap.put("dEp", dEp.getText());
				listItems.add(String.valueOf(jobMap.get("webtoonTitle") + " ("
						+ String.valueOf(jobMap.get("sEp") + " ~ " + String.valueOf(jobMap.get("dEp") + ")"))));

				// 실제 데이터를 담아둘 맵 정보
				Map<String, Object> fakeMap = new LinkedHashMap<String, Object>();
				fakeMap.put("webtoonTitle", titleField.getText());
				fakeMap.put("webtoonCode", codeField.getText());
				fakeMap.put("webtoonStartEp", sEp.getText());
				fakeMap.put("webtoonEndEp", dEp.getText());
				fakeList.add(fakeMap);
			}

		}

	}
	
	@FXML
	public void showHandleDialog() {
		try {

			// 다이얼로그 FXML 로드
			FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/NaverWebtoon.fxml"));
			AnchorPane page = (AnchorPane) loader.load();

			Stage dialog = new Stage();
			Scene scene = new Scene(page);
			dialog.setTitle("웹툰을 선택하세요.");
			dialog.initModality(Modality.WINDOW_MODAL);
			dialog.setResizable(false);
			dialog.setScene(scene);

			NaverWebtoonSelectService nav = null;
			List<NaverWebtoonSelectService> outputList = new ArrayList<NaverWebtoonSelectService>();
			List<Map<String, String>> webtoonList = ExcelReader.getExcelReader();

			for (int i = 0; i < webtoonList.size(); i++) {

				nav = new NaverWebtoonSelectService();
				Map<String, String> excelData = webtoonList.get(i);
				
				nav.setMonday(excelData.get("monday"));
				nav.setTuesday(excelData.get("tuesday"));
				nav.setWednesday(excelData.get("wednesday"));
				nav.setThursday(excelData.get("thursday"));
				nav.setFriday(excelData.get("friday"));
				nav.setSaturday(excelData.get("saturday"));
				nav.setSunday(excelData.get("sunday"));

				outputList.add(nav);

			}

			// 파싱한 데이터를 옵저블리스트에 담는다.
			ObservableList<NaverWebtoonSelectService> data = FXCollections.observableArrayList(outputList);

			// 서비스 컨트롤에 테이블에 보여주기 위한 데이터를 세팅한다.
			NaverWebtoonSelectController nwsController = loader.getController();
			nwsController.setDialogStageStage(dialog);
			nwsController.setMainController(root);
			nwsController.showWebtoonListDialog(data);

			// 다이얼로그를 보여준다.
			dialog.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 웹툰별 에피소드 입력
	 * 
	 * @param webCode
	 *            - 웹툰코드
	 */
	public void episodeSelect(String webCode) {
		try {

			// 첫 편 선택
			Document document = Jsoup.connect("http://comic.naver.com/webtoon/list.nhn?titleId=" + webCode).get();
			Elements elements = document.select("ul.btn_group li").eq(1);
			String[] parseTag = elements.html().split("return");

			int index = parseTag[1].indexOf(")");
			String functionName = parseTag[1].substring(0, index);

			String firstEpisode = functionName.split(",")[1].replaceAll("'", "");
			sEp.setText(firstEpisode);

			// 최근 편 선택
			elements = document.select("table.viewList > tbody > tr > td.title a").eq(0);
			String href = elements.attr("href");
			if ((!href.equals("")) || (href == null)) {
				String real_ep = href.split("no=")[1].split("&")[0];
				elements = document.select(".detail p");
				dEp.setText(real_ep);
			}

			sEp.setEditable(true);
			dEp.setEditable(true);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@FXML
	public void webtoonDownload() {

		if (sEp.getText().equals("") || dEp.getText().equals("")) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					AlertSupport alert = new AlertSupport("시작편수나, 종료편수는 필수값입니다.\n웹툰을 선택하세요.");
					alert.alertInfoMsg();
				}
			});
		} else {

			AlertSupport info = new AlertSupport("이 웹툰으로 다운로드 하시겠습니까?\n다운로드중에는 취소할 수 없습니다.");
			int count = info.alertConfirm();

			if (count > 0) {

				Task<Void> task = new Task<Void>() {
					public Void call() throws Exception {

						boolean merge_flag = false;

						// UI change code
						AlertSupport alert = new AlertSupport(root);
						alert.showDownloadModal();

						if (fakeList.size() == 0) {
							// 단일 다운로드

							// 시작편수
							int start = Integer.parseInt(sEp.getText());

							// 종료편수
							int end = Integer.parseInt(dEp.getText());

							// 웹툰코드
							String webCode = codeField.getText();

							String title = titleField.getText();

							for (int i = start; i <= end; i++) {

								Connection con = getConnection(webCode, i);
								con.timeout(5000);

								Document document = null;
								Elements elements = null;

								String dirPath = "";
								String path = "";

								List<String> hrefList = new ArrayList<String>();

								try {

									// HTML을 가져온다.
									document = con.get();

									// 웹툰 제목 파싱
									// title = document.getElementsByTag("title").html().split("::")[0];

									// 웹툰 실제편수 제목 (파일명으로 쓰임)
									String fileName = i + "화" + " - " + title;

									if (!title.equals("네이버만화 : 네이버웹툰")) {

										// 이 파일의 경로
										path = new File(".").getCanonicalPath();

										// 폴더구분이 체크안되어 있으면 파일명과 같은 폴더를 생성한다.
										if (!folderCheck.isSelected()) {
											dirPath = path + File.separator + "webtoons" + File.separator + title.trim()
													+ File.separator + fileName.trim();
										} else {
											// 폴더 구분이 체크되어있으면, 폴더를 생성하지 않는다.
											dirPath = path + File.separator + "webtoons" + File.separator
													+ title.trim();
										}

										// 다운로드 경로 객체생성
										File root = new File(dirPath);

										// 경로가 없으면
										if (!root.exists()) {
											// 새로 생성한다.
											root.mkdirs();
										}

										wdmLogReset(log);

										// 이미지 태그만 가져온다.
										elements = document.getElementsByTag("img");
										wdmLog(log, "제목: " + title + "[" + String.valueOf(i) + "] -> 다운로드를 시작 합니다.");

										// 반복문을 통해서 이미지 태그를 파싱한다.
										for (Element element : elements) {
											String imgTag = element.attr("src").toString();
											// 정규식으로 매칭되는 이미지만 선택한다.
											if (imgTag.matches(".*http://imgcomic.*")) {
												String src = imgTag.replaceAll(".net", ".com");

												// 다운로드 모듈을 통하여 다운로드 시작
												wgetDirectDownload(src, root.getPath());
												hrefList.add(src);
											}
										}

										String s_fileName = "";

										if (commentFlag) {

											wdmLog(log, "└> [베스트 댓글] -> 베스트 댓글을 생성중입니다.");

											// 해당 편수의 웹툰 댓글 텍스트를 가져와 이미지로 변환한다.
											TextImageConvert convert = new TextImageConvert();
											s_fileName = convert.textToImage(dirPath, fileName, webCode, i);

											if (s_fileName != null) {
												hrefList.add(s_fileName);
											}

										}

										// 이미지 병합 시작
										merge_flag = webtoonMergeStart(hrefList, dirPath, title, s_fileName, i);

										if (!merge_flag) {
											break;
										} else {
											wdmLog(log,
													"제목: " + title + "[" + String.valueOf(i) + "] -> 다운로드가 완료 되었습니다.");
											if (i == end) {
												alert.closeDownloadModal();
												// 시스템 종료 체크시
												if (systemCheck.isSelected()) {
													
													// 다운로드 완료시 즉시 시스템 종료
													String shutdownCmd = "shutdown -s";
													Runtime.getRuntime().exec(shutdownCmd);
												}
											}
										}

									}

								} catch (Exception e) {
									e.printStackTrace();
								}

							} // end for

						} else {
							// 예약 다운로드

							// 예약 리스트에 등록한 웹툰별 갯수 만큼 루프
							for (int i = 0; i < fakeList.size(); i++) {

								Map<String, Object> jobMap = fakeList.get(i);

								int start = Integer.parseInt(String.valueOf(jobMap.get("webtoonStartEp")));
								int end = Integer.parseInt(String.valueOf(jobMap.get("webtoonEndEp")));

								String webCode = String.valueOf(jobMap.get("webtoonCode"));
								String title = String.valueOf(jobMap.get("webtoonTitle"));

								for (int k = start; k <= end; k++) {

									Connection con = getConnection(webCode, k);
									con.timeout(5000);

									Document document = null;
									Elements elements = null;

									String dirPath = "";
									String path = "";

									List<String> hrefList = new ArrayList<String>();

									try {

										// HTML을 가져온다.
										document = con.get();

										// 웹툰 제목 파싱
										// title = document.getElementsByTag("title").html().split("::")[0];

										// 웹툰 실제편수 제목 (파일명으로 쓰임)
										String fileName = k + "화" + " - " + title;

										if (!title.equals("네이버만화 : 네이버웹툰")) {

											// 이 파일의 경로
											path = new File(".").getCanonicalPath();

											// 폴더구분이 체크안되어 있으면 파일명과 같은 폴더를 생성한다.
											if (!folderCheck.isSelected()) {
												dirPath = path + File.separator + "webtoons" + File.separator
														+ title.trim() + File.separator + fileName.trim();
											} else {
												// 폴더 구분이 체크되어있으면, 폴더를 생성하지 않는다.
												dirPath = path + File.separator + "webtoons" + File.separator
														+ title.trim();
											}

											// 다운로드 경로 객체생성
											File root = new File(dirPath);

											// 경로가 없으면
											if (!root.exists()) {
												// 새로 생성한다.
												root.mkdirs();
											}

											wdmLogReset(log);

											// 이미지 태그만 가져온다.
											elements = document.getElementsByTag("img");
											wdmLog(log,
													"제목: " + title + "[" + String.valueOf(k) + "] -> 다운로드를 시작 합니다.");

											// 반복문을 통해서 이미지 태그를 파싱한다.
											for (Element element : elements) {
												String imgTag = element.attr("src").toString();
												// 정규식으로 매칭되는 이미지만 선택한다.
												if (imgTag.matches(".*http://imgcomic.*")) {
													String src = imgTag.replaceAll(".net", ".com");

													// 다운로드 모듈을 통하여 다운로드 시작
													wgetDirectDownload(src, root.getPath());
													hrefList.add(src);
												}
											}

											String s_fileName = "";

											if (commentFlag) {

												wdmLog(log, "└> [베스트 댓글] -> 베스트 댓글을 생성중입니다.");

												// 해당 편수의 웹툰 댓글 텍스트를 가져와 이미지로
												// 변환한다.
												TextImageConvert convert = new TextImageConvert();
												s_fileName = convert.textToImage(dirPath, fileName, webCode, k);

												if (s_fileName != null) {
													hrefList.add(s_fileName);
												}

											}

											// 이미지 병합 시작
											merge_flag = webtoonMergeStart(hrefList, dirPath, title, s_fileName, k);

											if (!merge_flag) {
												break;
											} else {
												wdmLog(log, "제목: " + title + "[" + String.valueOf(k)
														+ "] -> 다운로드가 완료 되었습니다.");
											}

										}

									} catch (Exception e) {
										e.printStackTrace();
									}

								} // end 2 for

								if (i == fakeList.size() - 1) {
									alert.closeDownloadModal();
									
									// 시스템 종료 체크시
									if (systemCheck.isSelected()) {
										
										// 다운로드 완료시 즉시 시스템 종료
										String shutdownCmd = "shutdown -s";
										Runtime.getRuntime().exec(shutdownCmd);
									}
								}

							} // end 1 for

						}

						return null;
					}
				};

				Thread thread = new Thread(task);
				thread.start();

			}

		}

	}

	/* 이 하단의 코드들은 나중에 commons 패키지로 이동시킬 것 */

	/**
	 * 네이버 웹툰을 다운로드 받기 위해 연결함.
	 * 
	 * @param webCode
	 *            - 웹툰코드
	 * @param i
	 *            - 편수
	 * @return
	 */
	private Connection getConnection(String webCode, int i) {
		return Jsoup.connect("http://comic.naver.com/webtoon/detail.nhn?titleId=" + webCode + "&no=" + i);
	}

	/**
	 * wGet 모듈을 이용해 다운로드 구현
	 * 
	 * @param imgUrl
	 *            - 이미지 URL
	 * @param path
	 *            - 다운로드 경로
	 */
	private void wgetDirectDownload(String imgUrl, String path) {
		// 파일명 파싱
		int slashIndex = imgUrl.lastIndexOf('/');
		String fileName = imgUrl.substring(slashIndex + 1);
		try {

			URL url = new URL(imgUrl);
			File target = new File(path + File.separator + fileName);
			WGet w = new WGet(url, target);
			w.download();
		} catch (MalformedURLException e) {
		} catch (RuntimeException allDownloadExceptions) {
			allDownloadExceptions.printStackTrace();
		}

	}

	/**
	 * 조각난 이미지들을 하나로 합치는 메소드
	 * 
	 * @param imgList
	 *            - 이미지주소들이 담겨있는 리스트
	 * @param downloadDir
	 *            - 조각난 이미지들 다운로드 위치
	 * @param title
	 *            - 웹툰의 제목
	 */
	public boolean webtoonMergeStart(List<String> imgList, String downloadDir, String title, String s_fileName,
			int ep) {

		wdmLog(log, "└> 이미지 병합을 시작합니다. 잠시만 기다려주세요...");

		boolean flag = false;

		// 파싱된 파일명 목록을 담을 리스트
		List<String> fileList = new ArrayList<String>();

		try {

			// 이미지 최종높이 변수
			int maxheight = 0;

			String fileName = "";

			// 이미지 주소 리스트 루프시작
			for (int i = 0; i < imgList.size(); i++) {

				if (!imgList.get(i).matches(".*_s.*")) {
					// 이미지 주소를 파싱하여 파일명을 추려낸다.
					fileName = ((String) imgList.get(i)).toString()
							.substring(((String) imgList.get(i)).toString().lastIndexOf('/') + 1);
				} else {
					fileName = s_fileName;
				}

				// 지정된 위치에 해당 파일을 메모리에 읽어들인다.
				BufferedImage bufferFiles = ImageIO.read(new File(downloadDir + File.separator + fileName));

				// 읽어들인 이미지의 높이를 구한다.
				int height = bufferFiles.getHeight();

				// 이미지의 높이를 루프를 통해서 더해나간다.
				maxheight += height;

				// 파일목록 리스트에, 파일명을 하나씩 담는다.
				fileList.add(fileName);

			}

			BufferedImage image = null;

			/**
			 * 캔버스 이미지의 대표 가로너비를 지정하기 위한 이미지 객체 보통 0번째가 짧은 이미지가 있을 수 있어 3번째 이미지를
			 * 기준으로 한다.
			 */
			if (fileList.size() > 3) {
				image = ImageIO.read(new File(downloadDir + File.separator + (String) fileList.get(1)));
			} else {
				image = ImageIO.read(new File(downloadDir + File.separator + (String) fileList.get(0)));
			}

			// 이미지 최종높이만큼의 캔버스를 생성한다.
			BufferedImage canvasImage = new BufferedImage(image.getWidth(), maxheight, 1);

			wdmLog(log, "└> [이미지 병합] -> 캔버스가 생성되었습니다.");

			int max = 0;

			// 파일리스트 루프시작
			for (int i = 0; i < fileList.size(); i++) {

				// 파일 리스트에 들어있는 파일명을 메모리에 하나씩 읽어들인다.
				BufferedImage bufferImage = ImageIO
						.read(new File(downloadDir + File.separator + (String) fileList.get(i)));

				// 위에서 최종높이로 생성한 캔버스 객체를, 그래픽 객체로 변환한다.
				Graphics2D graphics = (Graphics2D) canvasImage.getGraphics();
				graphics.setBackground(Color.WHITE);

				if (i == 0) {
					// 0번째 이미지를 캔버스에 먼저 그린다.
					graphics.drawImage(bufferImage, 0, 0, null);
				} else {

					/**
					 * 1번째부터는 캔버스 위에 먼저 그려진 이전 이미지의 높이의 마지막부터 새로운 이미지를 그려야하므로,
					 * 이전 이미지의 높이를 구한다.
					 */
					int height = ImageIO.read(new File(downloadDir + File.separator + (String) fileList.get(i - 1)))
							.getHeight();

					// 구해진 이전 이미지의 높이를 더해나간다.
					max += height;

					// 이전 이미지 높이의 끝을 기준으로 새 이미지를 그려준다.
					graphics.drawImage(bufferImage, 0, max, null);
				}
			} // end for
			
			String saveImg = ep + "화 - " + title.trim() + ".png";
			wdmLog(log, "└> [이미지 병합] -> 파일을 생성하고 있습니다.");

			/**
			 * 저장할때 png로 생성하는 이유는 jpeg경우 픽셀의 최대가 65500인데, 웹툰의 이미지가 이를 초과하는 경우가
			 * 있다. 초과시에 png로 저장하지말고, 그냥 처음부터 png로 저장하자.
			 */
			ImageIO.write(canvasImage, "PNG", new File(downloadDir + File.separator + saveImg));

			// 이미지 생성후, 조각난 원본파일들을 삭제하자.
			File[] originalFiles = new File(downloadDir).listFiles();
			for (int i = 0; i < originalFiles.length; i++) {
				// 최종파일명이 아닌 파일들은 모두 삭제한다.
				if ((!originalFiles[i].toString().equals(downloadDir + File.separator + saveImg))
						&& (!originalFiles[i].getName().matches(".*" + title.trim() + ".*"))
						|| originalFiles[i].toString().equals(downloadDir + File.separator + s_fileName)) {
					originalFiles[i].delete();
				}
			}

			wdmLog(log, "└> [이미지 병합] -> 병합처리가 완료 되었습니다.");
			flag = true;

		} catch (Exception e) {
			flag = false;
			wdmLog(log, "└> [이미지 병합] -> 병합 중 오류가 발생했습니다.");
		}
		return flag;
	}

	/**
	 * 로그출력
	 * 
	 * @param log
	 *            - 로그 컴포넌트
	 * @param text
	 *            - 로그내용
	 */
	private void wdmLog(TextArea log, String text) {
		log.appendText("[INFO] " + text + "\n");
	}

	/**
	 * 로그 초기화
	 * 
	 * @param log
	 *            - 로그 컴포넌트
	 * @param text
	 *            - 로그내용
	 */
	private void wdmLogReset(TextArea log) {
		log.clear();
	}

}