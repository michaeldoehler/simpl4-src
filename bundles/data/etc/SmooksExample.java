import java.util.*;
import java.io.*;
import org.milyn.*;
import org.milyn.javabean.*;
import javax.xml.transform.stream.StreamSource;
import org.milyn.payload.*;
import flexjson.*;

public final class SmooksExample {

	private static JSONSerializer m_js = new JSONSerializer();

	public static void main(String args[]) throws Exception {
		Smooks smooks = new Smooks();
		//Smooks smooks = new Smooks("config.xml"); 
		Bean orderListBean = new Bean(HashSet.class, "orderlist", "/orderlist");
		Bean orderBean = new Bean(Order.class, "order", "/orderlist/order");
		orderListBean.bindTo(orderBean);
		//One-toOne 
		Bean headerBean = new Bean(Header.class, getRandomId(), "/orderlist/order/header");
		orderBean.bindTo("header", headerBean);
		headerBean.bindTo("customerNumber", "/orderlist/order/header/customer/@number");
		headerBean.bindTo("customerName", "/orderlist/order/header/customer");
		//One-toMany 
		Bean orderItemListBean = new Bean(HashSet.class, getRandomId(), "/orderlist/order/order-items");
		Bean orderItemBean = new Bean(OrderItem.class, getRandomId(), "/orderlist/order/order-items/order-item");
		orderBean.bindTo("orderItems", orderItemListBean);
		orderItemListBean.bindTo(orderItemBean);
		orderItemBean.bindTo("productId", "/orderlist/order/order-items/order-item/product");
		orderItemBean.bindTo("quantity", "/orderlist/order/order-items/order-item/quantity");
		orderItemBean.bindTo("price", "/orderlist/order/order-items/order-item/price");
		smooks.addVisitor(orderListBean);
		JavaResult result = new JavaResult();
		FileInputStream fis = new FileInputStream(new File("order.xml"));
		smooks.filterSource(new StreamSource(fis), result);
		Set orderlist = (Set) result.getBean("orderlist");
		System.out.println("OrderList:" + m_js.deepSerialize(orderlist));
	}

	public static String getRandomId() {
		return UUID.randomUUID().toString();
	}
}
