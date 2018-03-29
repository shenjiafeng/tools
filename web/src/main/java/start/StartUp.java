package start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages={"com.tool"})
public class StartUp {


  public static void main(String[] args)
  {
    SpringApplication.run(StartUp.class, args);
  }
}
