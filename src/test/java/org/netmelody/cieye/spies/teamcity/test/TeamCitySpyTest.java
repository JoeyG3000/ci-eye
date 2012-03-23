package org.netmelody.cieye.spies.teamcity.test;

import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.netmelody.cieye.core.domain.CiServerType;
import org.netmelody.cieye.core.domain.Feature;
import org.netmelody.cieye.core.domain.TargetDigest;
import org.netmelody.cieye.core.domain.TargetDigestGroup;
import org.netmelody.cieye.core.observation.CodeBook;
import org.netmelody.cieye.core.observation.CommunicationNetwork;
import org.netmelody.cieye.core.observation.Contact;
import org.netmelody.cieye.core.observation.KnownOffendersDirectory;
import org.netmelody.cieye.spies.teamcity.TeamCitySpy;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildType;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildTypeDetail;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildTypes;
import org.netmelody.cieye.spies.teamcity.jsondomain.Builds;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildsHref;
import org.netmelody.cieye.spies.teamcity.jsondomain.ProjectDetail;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public final class TeamCitySpyTest {

    private final Mockery context = new Mockery();
    
    private final CommunicationNetwork network = context.mock(CommunicationNetwork.class);
    private final KnownOffendersDirectory detective = context.mock(KnownOffendersDirectory.class);
    private final Contact contact = context.mock(Contact.class);
    
    @Before
    public void setup() {
        context.checking(new Expectations() {{
            allowing(network).makeContact(with(any(CodeBook.class))); will(returnValue(contact));
        }});
    }
    
    @Test public void
    givesEmptyStatusForAnUnknownEndpoint() {
        final TeamCitySpy spy = new TeamCitySpy("myEndpoint", network, detective);
        
        final TargetDigestGroup result = spy.targetsConstituting(new Feature("", "myOtherEndpoint", new CiServerType("TEAMCITY")));
        
        assertThat(result, is(Matchers.<TargetDigest>emptyIterable()));
    }
    
    @Test public void
    logsInUsingGuestAccess() {
        final TeamCitySpy spy = new TeamCitySpy("myEndpoint", network, detective);
        
        context.checking(new Expectations() {{
            allowing(contact).makeJsonRestCall(with(any(String.class)), with(BuildTypes.class));
                will(returnValue(new BuildTypes()));
            
            oneOf(contact).performBasicLogin("myEndpoint/guestAuth/");
        }});
        
        spy.targetsConstituting(new Feature("", "myEndpoint", new CiServerType("TEAMCITY")));
        
        context.assertIsSatisfied();
    }
    
    @Test public void
    lazilyRetrievesBuildTypeDetails() {
        final TeamCitySpy spy = new TeamCitySpy("myEndpoint", network, detective);
        
        context.checking(new Expectations() {{
            allowing(contact).makeJsonRestCall(with(any(String.class)), with(BuildTypes.class));
                will(returnValue(buildTypesForProjectsNamed("myFeatureName")));
            allowing(contact).makeJsonRestCall(with(any(String.class)), with(ProjectDetail.class));
                will(returnValue(projectNamed("myTarget")));
            
            never(contact).makeJsonRestCall(with(any(String.class)), with(BuildTypeDetail.class));
            
            ignoring(contact).performBasicLogin(with(any(String.class)));
        }});
        
        final TargetDigestGroup digest = spy.targetsConstituting(new Feature("myFeatureName", "myEndpoint", new CiServerType("TEAMCITY")));
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {{
            oneOf(contact).makeJsonRestCall(with(any(String.class)), with(BuildTypeDetail.class));
                will(returnValue(buildTypeDetail()));
            allowing(contact).makeJsonRestCall(with(any(String.class)), with(Builds.class));
                will(returnValue(new Builds()));
        }});
        
        spy.statusOf(digest.iterator().next().id());
        context.assertIsSatisfied();
    }

    private BuildTypeDetail buildTypeDetail() {
        final BuildTypeDetail detail = new BuildTypeDetail();
        detail.builds = new BuildsHref();
        return detail;
    }

    private BuildTypes buildTypesForProjectsNamed(String... names) {
        final BuildTypes buildTypes = new BuildTypes();
        buildTypes.buildType = newArrayList();
        
        for (String name : names) {
            final BuildType buildType = new BuildType();
            buildType.projectName = name;
            buildTypes.buildType.add(buildType);
        }
        return buildTypes;
    }
    
    private ProjectDetail projectNamed(String... names) {
        final ProjectDetail projectDetail = new ProjectDetail();
        projectDetail.buildTypes = new BuildTypes();
        projectDetail.buildTypes.buildType = newArrayList();
        
        for (String name : names) {
            final BuildType buildType = new BuildType();
            buildType.name = name;
            projectDetail.buildTypes.buildType.add(buildType);
        }
        return projectDetail;
    }
}
