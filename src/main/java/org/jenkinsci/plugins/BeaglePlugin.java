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
import hudson.util.Secret;

public class BeaglePlugin extends Builder implements SimpleBuildStep {

	Secret atoken;
	Secret utoken;

	@DataBoundConstructor
	public BeaglePlugin(Secret apptoken,Secret usertoken) {
		this.atoken = apptoken;
		this.utoken = usertoken;
	}

	@Exported
    public String getApptoken() {
        return atoken.toString();
    }
    @Exported
    public String getUsertoken() {
       return utoken.toString();
    }

	public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
       	Secret gtoken = getDescriptor().getUtoken();
       	boolean flag = true;
       	boolean guflag = false;
       	if (atoken.toString().isEmpty()) {
       		listener.getLogger().println("Application Token not Provided! Refer Help File");
       		flag = false;
       	}
      	if (utoken.toString().isEmpty()) {
      		if (gtoken.toString().isEmpty()) {
      			listener.getLogger().println("User Token not provided by globally or locally! Refer Help");
       			flag = false;
      		} else {
      			guflag = true;
      			utoken = gtoken;
      		}
       	}
       	if(flag) {
			HttpClient c = HttpClientBuilder.create().build();
			HttpPost p = new HttpPost("https://api.beaglesecurity.com/v1/test/start/");
			p.setEntity((HttpEntity) new StringEntity("{\"user_token\":\""+utoken.toString()+"\",\"application_token\":\""+atoken.toString()+"\"}",ContentType.create("application/json")));
	        HttpResponse r = null;
			try {
				String str = null;
				r = c.execute(p);
				int statcode = r.getStatusLine().getStatusCode();
				if(statcode == 200 || statcode == 400) {
					BufferedReader rd = new BufferedReader(new InputStreamReader(r.getEntity().getContent()));
					str = rd.readLine();
					JsonParser parser = new JsonParser();
					if (str != null) {
						JsonElement jsonel = parser.parse(str);
						JsonObject obj = jsonel.getAsJsonObject();
						listener.getLogger().println("Status :" + obj.get("status"));
						listener.getLogger().println("Message :" + obj.get("message"));
						if(guflag) {
							utoken = null;
							guflag = false;
						}
					}
				} else  {
					listener.getLogger().println("Error Code :"+statcode);
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
		private Secret gutoken;

		public DescriptorImpl() {
            load();
        }
        @Exported
    	public String getGusertoken() {
       		return gutoken.toString();
    	}
		public String getDisplayName() {
    		return "Trigger Beagle Penetration Testing";
		}

		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
    		gutoken = Secret.fromString(formData.getString("gusertoken"));
    		save();
    		return super.configure(req,formData);
		}
		public Secret getUtoken() {
            return gutoken;
        }
	}
}
