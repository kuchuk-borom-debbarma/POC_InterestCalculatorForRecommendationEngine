package dev.kuku.interestcalculator.filter;

import dev.kuku.interestcalculator.dto.OperationDetailMap;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@WebFilter("/*") // Apply to all requests
public class OperationDetailMapClearFilter implements Filter {

    @Autowired
    private OperationDetailMap operationDetailMap;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Clear the map at the beginning of each request
        operationDetailMap.operationDetailMap.clear();

        // Continue with the filter chain
        chain.doFilter(request, response);
    }
}