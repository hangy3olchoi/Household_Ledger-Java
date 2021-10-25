package stream_service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import homebook.dao.HomeBookDAO;
import homebook.dao.IDao;
import homebook.tools.ConnectionFactory;
import homebook.vo.HomeBook;
import oracle.net.aso.h;

// 복잡한 질의없이 Stream을 이용한 서비스 개발 예제
public class Service {
	private Connection conn;
	private IDao dao;
	
	// 서비스 생성 메소드
	public Service() {
		try {
			this.conn = ConnectionFactory.create();
			this.dao = new HomeBookDAO();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// service test
	public static void main(String[] args) throws SQLException {
		Service service = new Service();
		Connection conn = ConnectionFactory.create();
		HomeBookDAO dao = new HomeBookDAO();
		service.test(service.conn,service.dao);
		
	}
	// 범용 서비스를 위한 메소드 만들어 보기
	
	// allData 얻는 메소드 만들기.
	public List<HomeBook> getAllData(){
		List<HomeBook> data = null;
		
		try {
			data = dao.selectAll();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return data;
	}
	
	// 지정한 날짜의 자료를 출력해주는 서비스
	public List<HomeBook> getSomedayData(int year, int month, int day){
		Predicate<HomeBook> pre = h ->{ 
			Timestamp ts = h.getDay();
			boolean a = ts.getYear() == year - 1900;
			boolean b = ts.getMonth() == month-1;
			boolean c = ts.getDate() == day;
			
			return a && b && c;
		};
		// filter시 사용하는 타입은 항상 Predicate
		return getAllData().stream().filter(pre).collect(Collectors.toList());
	}	

	public List<HomeBook> getTermData(Timestamp a, Timestamp b){
		Predicate<HomeBook> pre = h ->{ 
			Timestamp ts = h.getDay();
			return ((ts.after(a) && ts.before(b)) || ts.equals(a) || ts.equals(b));
		};
		return getAllData().stream().filter(pre).collect(Collectors.toList());
	}
	// 지정한 금액 이상의 지출을 한 목록
	public List<HomeBook> moreThanData(long amount){
		return getAllData().stream().filter(h -> h.getSection().equals("Expense")).filter(h -> h.getExpense() >= amount).collect(Collectors.toList());
	}
	// 지정한 계정과목 데이터 목록
	public List<HomeBook> certainAccountTitle(String title){
		return getAllData().stream().filter(h -> h.getAccounttitle().equals(title)).collect(Collectors.toList());
		
	}
	// 주어진 지출 계정 과목만 출력하기
	public Long certainAccountTitleSum(String title){
		return getAllData().stream()
				.filter(h -> h.getAccounttitle().equals(title))
				.mapToLong(h -> (h.getSection().equals("Revenue"))? h.getRevenue() : h.getExpense())
				.sum();
	}
	// 사용된 계정과목 추출하기
	public List<String> getUsedTitle(){
		return getAllData().stream().map(h -> h.getAccounttitle()).distinct().collect(Collectors.toList());
	}
	public void sumPrintService() {
		List<String> usedT = getUsedTitle();
		for(String x : usedT) {
			System.out.println(x+":"+certainAccountTitleSum(x));
		}
	}
	
	
	// 가계부 데이터를 계정과목별로 분류하여 집계출력 하는 서비스
	public int test(Connection conn, IDao dao) {
		int res = 0;
		
		try {
			// dao에서 selectAll만 가져와서 List에 담음.
			List<HomeBook> allData = dao.selectAll();
			allData.stream().forEach(x->System.out.println(x));

			// 차변(수입)합계
			long 차변합계 = allData.stream().mapToLong(h -> h.getRevenue()).sum();
			// 대변(지출)합계
			long 대변합계 = allData.stream().mapToLong(h -> h.getExpense()).sum();
			
			System.out.println(" ↓ 사용한 지출과목 리스트---------------------------------------------------------------------------------------------------------");

			// 사용한 지출과목 리스트 얻기
			getUsedTitle().stream().forEach(System.out::println);
			
			System.out.println();
			System.out.println(" ↓ 지정한 날짜의 가계부----------------------------------------------------------------------------------------------------------");
			
			// 지정한 날짜의 가계부 목록들
			getSomedayData(2021,6,28).stream().forEach(System.out::println);
			
			System.out.println();
			System.out.println(" ↓ 지정한 기간의 가계부----------------------------------------------------------------------------------------------------------");
			
			// 지정한 기간의 가계부 목록들
				//LocalData a = new LocalData("2021-6-1");
			Timestamp a = new Timestamp(121,5,29,0,0,0,0);
			Timestamp b = new Timestamp(121,6,01,0,0,0,0);
			getTermData(a,b).stream().forEach(System.out::println);
			
			System.out.println();
			System.out.println(" ↓ 지출금액이 일정액 초과인 목록---------------------------------------------------------------------------------------------------");
			
			// 지출금액이 50000원 초과인 목록들
			//allData.stream().filter(h -> h.getSection().equals("Expense")).filter(h -> h.getExpense()>50000).forEach(System.out::println);
			moreThanData(50000).stream().forEach(System.out::println);
			
			System.out.println();
			System.out.println(" ↓ 주어진 지출 계정 과목만 출력----------------------------------------------------------------------------------------------------");
			
			// 주어진 지출 계정 과목만 출력하기
			certainAccountTitle("지출_주택관리비").forEach(h -> System.out.println(h));
			
			System.out.println();
			System.out.println(" ↓ 주어진 지출 계정 과목의 합계----------------------------------------------------------------------------------------------------");
			
			// 주어진 지출 계정 과목의 합계
			System.out.println("식대 지출계: "+certainAccountTitleSum("지출_식대"));
			
			System.out.println();
			System.out.println(" ↓ 집계--------------------------------------------------------------------------------------------------------------------------");
			sumPrintService();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}

}
