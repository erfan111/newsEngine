package ir.open30stem.controllers;

import ir.open30stem.models.*;
import ir.open30stem.searchEngine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Created by Erfan Sharafzadeh on 11/17/16.
 */
@RestController
public class ApplicationController {

    @Autowired
    private NewsItemRepository newsItemRepository;

    @Autowired
    private SpiderRepository spiderRepository;
    private InvertedIndex invertedIndex;

    @PostConstruct
    public void init() {
        invertedIndex = new InvertedIndex(newsItemRepository);
    }

    @RequestMapping(method = GET, value = "/hello")
    public String hello() {
        return "Hello world from Spring!";
    }

    @RequestMapping(method = POST, value = "/index")
    public ResponseEntity<String> index() {
        try {
            invertedIndex.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>("News Articles indexed successfully!",HttpStatus.OK);
    }

    @RequestMapping(value = "/news/{count}")
    public ResponseEntity<List<News>> news(@PathVariable  Integer count) {
        List<newsItem> list = newsItemRepository.findAllLimit(new PageRequest(0, count));
        List<News> result = list.stream().map(newsItem::toNews).collect(Collectors.toList());
        System.out.println(newsItemRepository.findAll().size());
        return new ResponseEntity<>(result, HttpStatus.OK);

    }

    @RequestMapping(method = POST, value = "/query")
    public ResponseEntity<List<News>> search(@RequestBody QueryString q){
        List<newsItem> results;
        List<News> result = new ArrayList<>();
        try {
            if (!invertedIndex.indexed)
                invertedIndex.read();
            results = invertedIndex.query(false,false,q.query, q.weights);
            result = results.stream().map(newsItem::toNews).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(method = GET, value = "/news/press/{p}")
    public ResponseEntity<List<News>> searchByPress(@PathVariable String p){
        List<newsItem> list = newsItemRepository.findByPressAndFinishedOrderByNewsIdDesc(p, true, new PageRequest(0, 10));
        List<News> result = list.stream().map(newsItem::toNews).collect(Collectors.toList());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(method = GET, value = "/news/id/{id}")
    public ResponseEntity<News> searchById(@PathVariable String id){
        newsItem item = newsItemRepository.findById(id);
        if(item != null)
            return new ResponseEntity<>(item.toNews(), HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(method = GET, value = "/news/category/{c}")
    public ResponseEntity<List<News>> searchByCategory(@PathVariable String c){
        List<newsItem> list = newsItemRepository.findByCategoryAndFinishedOrderByNewsIdDesc(c, true, new PageRequest(0, 10));
        List<News> result = list.stream().map(newsItem::toNews).collect(Collectors.toList());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(method = GET, value = "/press")
    public ResponseEntity<List<Spider>> getPress(){
        List<Spider> list = spiderRepository.findAll();
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

}
