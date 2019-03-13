package myapplication.service.service.impl;

import ch.qos.logback.core.encoder.EchoEncoder;
import com.googlecode.protobuf.format.JsonFormat;
import com.khoi.proto.CreateRequest;
import com.khoi.proto.CreateResponse;
import com.khoi.proto.DeleteRequest;
import com.khoi.proto.DeleteResponse;
import com.khoi.proto.GetPriceHistoryRequest;
import com.khoi.proto.GetPriceRequest;
import com.khoi.proto.GetPriceResponse;
import com.khoi.proto.PriceEntry;
import com.khoi.proto.PriceServiceGrpc;
import com.khoi.stockproto.GetStockRequest;
import com.khoi.stockproto.GetStockResponse;
import com.khoi.stockproto.StockServiceGrpc;
import com.khoi.supplierproto.GetSupplierListRequest;
import com.khoi.supplierproto.SupplierEntry;
import com.khoi.supplierproto.SupplierServiceGrpc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import myapplication.dao.IProductDAO;
import myapplication.dto.Product;
import myapplication.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements IProductService {

  @Qualifier("priceService")
  private final PriceServiceGrpc.PriceServiceBlockingStub priceService;

  @Qualifier("stockService")
  private final StockServiceGrpc.StockServiceBlockingStub stockService;

  @Qualifier("supplierService")
  private final SupplierServiceGrpc.SupplierServiceBlockingStub supplierService;

  @Autowired
  private IProductDAO productDAO;

  public ProductServiceImpl(PriceServiceGrpc.PriceServiceBlockingStub priceService,
      StockServiceGrpc.StockServiceBlockingStub stockService,
      SupplierServiceGrpc.SupplierServiceBlockingStub supplierService) {
    this.priceService = priceService;
    this.stockService = stockService;
    this.supplierService = supplierService;
  }

  private static <E> Collection<E> makeCollection(Iterable<E> iter) {
    Collection<E> list = new ArrayList<E>();
    for (E item : iter) {
      list.add(item);
    }
    return list;
  }

  private static <T> Iterable<T> toIterable(final Iterator<T> iterator) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return iterator;
      }
    };
  }

  @Override
  public List<Product> findAll() {
    //return productDAO.findAll();
    List<Product> list = productDAO.findAll();
    for (Product prod : list) {
      //get price
      prod = findByid(prod.getId());
    }
    return list;
  }

  @Override
  public Product findByid(int id) {
    Product prod = productDAO.findByid(id);
    try {
      //get PriceHistory
      Iterable<PriceEntry> entries = toIterable(priceService.getPriceHistory(
          GetPriceHistoryRequest.newBuilder().setProductId(id).build()));

      List<PriceEntry> list1 = new ArrayList<>();
      entries.forEach(list1::add);

      //cach ta dao
    /*List<String> strings = list1.stream()
        .map(object -> Objects.toString(object, null))
        .collect(Collectors.toList());*/

      List<String> strings = new ArrayList<>();

      //cach khong ta dao?
      for (PriceEntry price : list1) {
        strings.add(new JsonFormat().printToString(price));
      }

      prod.setPriceEntries(strings);

      GetPriceResponse rs = priceService
          .getPrice(GetPriceRequest.newBuilder().setProductId(id).build());
      prod.setPrice(rs.getPrice());
    } catch (Exception ex) {
      System.out.println(ex);
      prod.setPriceEntries(null);
      prod.setPrice(-1);
    }
    try {
      //get stock
      GetStockResponse rs2 = stockService
          .getStock(GetStockRequest.newBuilder().setProductId(id).build());
      prod.setStock(rs2.getStock());
    } catch (Exception ex) {
      prod.setStock(-1);
    }
    try {
      //get list of suppliers selling this product
      List<SupplierEntry> supplierEntryList = new ArrayList<>();
      List<String> supplierList = new ArrayList<>();//result of list<entry> -> list<String>
      //get result from gRPC server
      Iterable<SupplierEntry> supplierEntryIterable = toIterable(
          supplierService.getSupplierListByProductId(
              GetSupplierListRequest.newBuilder().setProductId(id).build()));
      //convert Iterable -> list<Entry>
      supplierEntryIterable.forEach(supplierEntryList::add);
      //convert list<entry> -> list<String>
      for (SupplierEntry supplierEntry : supplierEntryList) {
        supplierList.add(new JsonFormat().printToString(supplierEntry));
      }

      prod.setSupplierEntries(supplierList);
    } catch (Exception ex) {
      prod.setSupplierEntries(null);
    }
    return prod;
  }

  @Override
  public Boolean create(Product product) {
    if (productDAO.create(product)) {

      //create new price
      CreateResponse rs = priceService.create(
          CreateRequest.newBuilder().setPrice(product.getPrice()).setProductId(product.getId())
              .build());
      if (rs.getId() > 0) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public Boolean update(Product product) {
    Product prod_old = findByid(product.getId());

    if (prod_old.getPrice() != product.getPrice()) {
      CreateResponse rs = priceService.create(
          CreateRequest.newBuilder().setPrice(product.getPrice()).setProductId(product.getId())
              .build());
      if (rs.getId() >= 0) {
        if (productDAO.update(product)) {
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public Boolean delete(int id) {
    if (productDAO.delete(id)) {
      DeleteResponse rs = priceService.delete(DeleteRequest.newBuilder().setProductId(id).build());
      return true;
    } else {
      return false;
    }
  }
}
