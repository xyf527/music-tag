package com.xin.musictag.web;

import com.xin.musictag.application.BatchImportService;
import com.xin.musictag.domain.BatchTask;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class BatchController {
 private final BatchImportService batches;
 public BatchController(BatchImportService batches){this.batches=batches;}
 @GetMapping("/batch") public String page(){return "batch";}
 @GetMapping("/api/batches/policy") @ResponseBody public Map<String,Object> policy(){return batches.policy();}
 @PostMapping(value="/api/batches",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseBody public BatchTask upload(@RequestParam("files") List<MultipartFile> files,@RequestParam("paths") List<String> paths){return batches.importFiles(files,paths);}
 @GetMapping("/api/batches/{id}") @ResponseBody public Map<String,Object> detail(@PathVariable long id){return batches.detail(id);}
 @PostMapping("/api/batches/{id}/confirm") @ResponseBody public BatchTask confirm(@PathVariable long id,@RequestParam(required=false) Set<Long> skipIds){return batches.confirm(id,skipIds==null?Set.of():skipIds);}
 @PostMapping("/api/batches/{id}/execute") @ResponseBody public void execute(@PathVariable long id){batches.execute(id,false);}
 @PostMapping("/api/batches/{id}/retry") @ResponseBody public void retry(@PathVariable long id){batches.execute(id,true);}
 public record BindingRequest(Long lyricsItemId, Long coverItemId) { }
 @PostMapping("/api/batches/{id}/items/{itemId}/bindings") @ResponseBody public void bind(@PathVariable long id, @PathVariable long itemId, @RequestBody BindingRequest request){batches.bind(id,itemId,request.lyricsItemId(),request.coverItemId());}
 @GetMapping("/api/batches/{id}/report") @ResponseBody public Map<String,Object> report(@PathVariable long id){return batches.detail(id);}
 @GetMapping("/api/batches/{id}/zip") public ResponseEntity<StreamingResponseBody> zip(@PathVariable long id){
  batches.assertDownloadable(id);
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename("batch-"+id+".zip").build().toString()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(output -> batches.writeZip(id,output));
 }
}
