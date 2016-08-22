package org.jenkinsci.plugins.vault;

import com.bettercloud.vault.VaultConfig;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.credentialsbinding.Binding;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;

import org.jenkinsci.plugins.vault.api.VaultApi;
import org.jenkinsci.plugins.vault.api.VaultApiFactory;
import org.jenkinsci.plugins.vault.config.VaultServerConfigImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.bettercloud.vault.VaultException;
import java.io.IOException;

/**
 * Extension for credential-bindings to map a vault secret to an env variable
 */
public class SecretBinding extends Binding<SecretCredentials> {

    private String secretField;
    private VaultServerConfigImpl vaultConfig;

    @DataBoundConstructor
    public SecretBinding(String variable, String credentialsId) {
        super(variable, credentialsId);
    }

    public String getSecretField() {
        return secretField;
    }

    @DataBoundSetter
    public void setSecretField(String secretField) {
        this.secretField = secretField;
    }

    public VaultServerConfigImpl getVaultConfig() {
        return vaultConfig;
    }

    @DataBoundSetter
    public void setVaultConfig(VaultServerConfigImpl vaultConfig) {
        this.vaultConfig = vaultConfig;
    }

    @Override protected Class<SecretCredentials> type() {
        return SecretCredentials.class;
    }

    @Override public SingleEnvironment bindSingle(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        String secretPath = getCredentials(build).getSecretPath().getPlainText();
        VaultApi vaultApi = VaultApiFactory.create(vaultConfig, build);
        String secretValue;
        try {
            secretValue = vaultApi.readField(secretPath, secretField);
        }
        catch(VaultException e) {
          throw new IOException(e.getMessage());
        }

        SingleEnvironment env;
        try {
          env = new SingleEnvironment(secretValue);
        }
        catch(NullPointerException e){
          NullPointerException f = new NullPointerException( "Got NullPointer from Vault, have you set the secret in Vault? \n"+e.getMessage());
          f.setStackTrace(e.getStackTrace());
          throw f;
        }
        return env
    }

    @Extension
    public static class DescriptorImpl extends BindingDescriptor<SecretCredentials> {

        @Override protected Class<SecretCredentials> type() {
            return SecretCredentials.class;
        }

        @Override public String getDisplayName() {
            return "Vault binding";
        }
    }
}
