podTemplate(label: 'testing',
        containers: [
                containerTemplate(name: 'kubectl', image: 'jorgeacetozi/kubectl:1.7.0', ttyEnabled: true, command: 'cat'),
                containerTemplate(name: 'mysql', image: 'mysql:5.7', envVars: [
                        envVar(key: 'MYSQL_DATABASE', value: 'notepad'),
                        envVar(key: 'MYSQL_ROOT_PASSWORD', value: 'root')
                ]),
                containerTemplate(name: 'maven', image: 'jorgeacetozi/maven:3.5.0-jdk-8-alpine', ttyEnabled: true, command: 'cat'),
                containerTemplate(name: 'docker', image: 'docker', ttyEnabled: true, command: 'cat')
        ],
        volumes: [
                hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                secretVolume(mountPath: '/etc/maven/', secretName: 'maven-settings-secret')
        ],
        envVars: [
                secretEnvVar(key: 'DOCKERHUB_USERNAME', secretName: 'dockerhub-username-secret', secretKey: 'USERNAME'),
                secretEnvVar(key: 'DOCKERHUB_PASSWORD', secretName: 'dockerhub-password-secret', secretKey: 'PASSWORD'),
        ])
        {
            node('testing') {
                def image_name = "notepad"

                checkout scm

                dir('app') {
                    stage('Checkout the Notepad application') {
                        container('maven') {
                            git url: 'git@github.com:adidhab/notepad.git', branch: "${GIT_BRANCH}"
                        }
                    }

                    stage('Run Unit/Integration Tests, generate the jar artifact and push it to Artifactory') {
                        container('maven') {
                            sh 'mvn -B -s /etc/maven/settings.xml clean deploy'
                        }
                    }

                    stage('Build and push a new Docker image with the tag based on the Git branch') {
                        container('docker') {
                            sh """
            docker login -u ${DOCKERHUB_USERNAME} -p ${DOCKERHUB_PASSWORD}
            docker build -t ${DOCKERHUB_USERNAME}/${image_name}:${GIT_BRANCH} .
            docker push ${DOCKERHUB_USERNAME}/${image_name}:${GIT_BRANCH}
          """
                        }
                    }
                }
            }
        }
