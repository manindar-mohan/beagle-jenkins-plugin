package org.jenkinsci.plugins;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import org.apache.http.entity.ContentType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.kohsuke.stapler.export.Exported;


public class BeaglePlugin extends Builder implements SimpleBuildStep {

	String atoken;
	String utoken;

	@DataBoundConstructor
	public BeaglePlugin(String apptoken,String usertoken) {
		this.atoken = apptoken;
		this.utoken = usertoken;
	}

	@Exported
    public String getApptoken() {
        return atoken;
    }
    @Exported
    public String getUsertoken() {
       return utoken;
    }
    @Exported
    public String getGusertoken() {
       return getDescriptor().getUtoken().toString();
    }

	public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
       	String gtoken = getDescriptor().getUtoken().toString();
       	boolean flag = true;
       	boolean guflag = false;
       	if (atoken == null || atoken.isEmpty()) {
       		listener.getLogger().println("Application Token not Provided! Refer Help File");
       		flag = false;
       	}
      	if (utoken.isEmpty()) {
      		if (gtoken.isEmpty()) {
      			listener.getLogger().println("User Token not provided by globally or locally! Refer Help");
       			flag = false;
      		} else {
      			guflag = true;
      			utoken = gtoken;
      		}
       	}
		if(flag) {
			String body = "{\"user_token\":\""+utoken+"\",\"application_token\":\""+atoken+"\"}";
			HttpClient c = HttpClientBuilder.create().build();   
			HttpPost p = new HttpPost("https://api.beaglesecurity.com/v1/test/start/");        
			p.setEntity((HttpEntity) new StringEntity(body,ContentType.create("application/json")));
	        HttpResponse r = null;
			try {
				String str = null;
				r = c.execute(p);
				BufferedReader rd = new BufferedReader(new InputStreamReader(r.getEntity().getContent()));
				str = rd.readLine();
				JsonParser parser = new JsonParser();
				if (str != null) {
					JsonElement jsonel = parser.parse(str);
					JsonObject obj = jsonel.getAsJsonObject();
					listener.getLogger().println("Status :" + obj.get("status"));
					listener.getLogger().println("Message :" + obj.get("message"));
					if(guflag) {
						utoken = "";
						guflag = false;
					} 
				}

			} catch (IOException e) {
			}
		}
		 
    }
	public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
	}
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
		}
		private String gutoken;

		public DescriptorImpl() {
            load();
        }
        @Exported
    	public String getGusertoken() {
       		return gutoken;
    	}
		public String getDisplayName() {
    		return "Trigger Beagle Penetration Testing";
		}

		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
    		gutoken = formData.get("gusertoken").toString();
    		save();
    		return super.configure(req,formData);
		}
		public String getUtoken() {
            return gutoken;
        }
	}
}
